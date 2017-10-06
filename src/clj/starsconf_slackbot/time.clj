(ns starsconf-slackbot.time
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))


(def TIMEZONE-SCL (t/time-zone-for-offset -3))


(defn now []
  (t/to-time-zone (t/now) TIMEZONE-SCL))


(defn to-seconds [datetime]
  "Converts datetime to seconds since epoch."
  (t/in-seconds (t/interval (t/epoch) datetime)))


(defn parse-datetime [date time]
  (let [formatter (f/formatters :date-hour-minute-second)
        datetime    (f/parse formatter (str date "T" time))]
    (t/from-time-zone datetime TIMEZONE-SCL)))


(defn equal-or-after [d1 d2]
  (or (= d1 d2)
      (t/after? d1 d2)))
