(ns starsconf-slackbot.db
  (:require [codax.core :as codax]))

;; Simple database stored in as a single file.
(defonce db (codax/open-database "data/teams-data"))


(defn bot-id [team-id]
  (codax/get-at! db [:teams team-id :bot :bot_user_id]))

(defn bot-token [team-id]
  (codax/get-at! db [:teams team-id :bot :bot_access_token]))

(defn teams []
  (codax/get-at! db [:teams]))

(defn team-id [bot-id]
  (let [filter-fn (fn [[team-id team]]
                    (= bot-id
                       (-> team :bot :bot_user_id)))]
    (-> (filter filter-fn (teams)) first first)
    ))

(defn add-team [team-id team-data]
  (codax/assoc-at! db [:teams team-id] team-data))

(defn add-subscription [channel-id bot-id]
  (codax/assoc-at! db [:subscriptions channel-id] bot-id))

(defn cancel-subscription [channel-id]
  (codax/assoc-at! db [:subscriptions channel-id] false))

(defn subscriptions []
  (codax/get-at! db [:subscriptions]))

(defn save-response-data [data]
  (codax/assoc-at! db [:response-data] data))

(defn get-response-data []
  (codax/get-at! db [:response-data]))


