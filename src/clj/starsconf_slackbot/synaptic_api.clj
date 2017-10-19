(ns starsconf-slackbot.synaptic-api
  (:require [clj-http.client :as client]
            [venia.core :as v]
            [clojure.core.memoize :as memo]
            [starsconf-slackbot.time :as time]
            [starsconf-slackbot.db :as db]
            ))


(def GRAPHQL-URL "https://api-starsconf.synaptic.cl/graphql")


(defn- api-request
  "If request is successful, store response in db, else, get data from db."
  [graphql-query]
  (let [query-body (str "{\"query\":" " \"" graphql-query "\"}")
        request (client/post GRAPHQL-URL {:body query-body
                                          :content-type :json
                                          :as :json
                                          :throw-exceptions false})]
    (if (= (:status request) 200)
      (db/save-response-data (-> request :body :data))
      (db/get-response-data)
      )))


(defn- all-events-request
  "Request all events from synaptic's api. Returns nil if the request fails."
  []
  (let [query-fields [[:allTalks [:id
                                  :name
                                  [:speaker
                                   [:name ]]
                                  [:timeSlot
                                   [:date :start :end]]
                                  :room
                                  :category
                                  :isPlaceholder]]]
        graphql-query (v/graphql-query {:venia/queries query-fields})]
    (:allTalks (api-request graphql-query))))


(defn- event-to-datetime [event]
  (let [date       (-> event :timeSlot :date)
        start-time (-> event :timeSlot :start)]
    (time/parse-datetime date start-time)))


(defn- event-comparator [e1 e2]
  (let [e1-seconds (-> e1 :datetime time/to-seconds)
        e2-seconds (-> e2 :datetime time/to-seconds)]
    (compare e1-seconds e2-seconds)))


(defn- sort-events-by-date
  "Sort events by start datetime."
  [events]
  (let [events-with-datetime
        (map (fn [event] (assoc event :datetime (event-to-datetime event)))
             events)]
    (sort event-comparator events-with-datetime)))


(def all-events
  "Same as all-events-request, but sorts the events, caches the results for 3600 seconds
   (memo/ttl returns a function)."
  (let [threshold (* 3600 1000)]
    (memo/ttl #(sort-events-by-date (all-events-request)) :ttl/threshold threshold)))


(defn all-talks
  "A talk is an event that is not placeholder."
  []
  (let [events (all-events)]
    (filter (fn [event] (not (:isPlaceholder event))) events)))


(defn all-categories []
  (let [talks (all-talks)
        categories (map (fn [talk] (:category talk)) talks)]
    (filter not-empty (set categories))))


(defn all-rooms []
  (let [talks (all-talks)
        categories (map (fn [talk] (:room talk)) talks)]
    (filter not-empty (set categories))))


(defn next-event-from
  "Returns nearest event in time from datetime, or nil if datetime occurs after
   the last event."
  [datetime]
  (loop [events (all-events)]
    (if (empty? events)
      nil
      (if (time/equal-or-after (event-to-datetime (first events)) datetime)
        (first events)
        (recur (rest events))))))


(defn next-n-events-from
  "Get a list of (at most) n events starting from datetime."
  [n datetime]
  (loop [c n events (all-events) result []]
    (if (or (empty? events) (<= c 0))
      result
      (if (time/equal-or-after (event-to-datetime (first events)) datetime)
        (recur (dec c) (rest events) (conj result (first events)))
        (recur c (rest events) result))
      )))


(defn get-events-within-next-hour
  "Get all events within [hour, hour+1]"
  [datetime]
  (let [events (all-events)
        filter-fn (fn [event]
                    (time/within-next-hour? datetime (:datetime event)))]
    (filter filter-fn events)))



