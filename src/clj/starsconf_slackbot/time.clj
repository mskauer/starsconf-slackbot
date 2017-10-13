(ns starsconf-slackbot.time
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))


(def TIMEZONE-SCL (t/time-zone-for-offset -3))


(defn now []
  (t/to-time-zone (t/now) TIMEZONE-SCL))


(defn to-seconds
  "Converts datetime to seconds since epoch."
  [datetime]
  (t/in-seconds (t/interval (t/epoch) datetime)))


(defn parse-datetime
  "Given two strings with formats yyyy-mm-dd and HH:MM:SS, returns
   a Joda datetime object with timezone TIMEZONE-SCL"
  [date time]
  (let [formatter (f/formatters :date-hour-minute-second)
        datetime    (f/parse formatter (str date "T" time))]
    (t/from-time-zone datetime TIMEZONE-SCL)))


(defn set-hour
  "Sets datetime's hour to hour, and minutes and seconds to zero."
  [datetime hour]
  (let [new-datetime (t/date-time (t/year datetime)
                                  (t/month datetime)
                                  (t/day datetime)
                                  hour)]
    (t/from-time-zone new-datetime TIMEZONE-SCL)))


(defn equal-or-after [d1 d2]
  (or (= d1 d2)
      (t/after? d1 d2)))


(defn within-next-hour? [start datetime]
  (let [end      (t/plus start (t/minutes 59))
        interval (t/interval start end)]
    (t/within? interval datetime)))

(defn minus5-minutes [datetime]
  (t/minus datetime (t/minutes 5)))


