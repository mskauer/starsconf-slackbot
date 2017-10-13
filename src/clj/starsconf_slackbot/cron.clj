(ns starsconf-slackbot.cron
  (:require [chime :refer [chime-at]]
            [starsconf-slackbot.time :as time]
            [starsconf-slackbot.synaptic-api :as api]
            [starsconf-slackbot.db :as db]
            [starsconf-slackbot.bot.slackbot :as slackbot]
            [clojure.tools.logging :as log]
            ))


(defn notify-subscriptors [event]
  (let [;; events (api/all-events)
        ;; event (first (filter #(= (:id %) event-id) events))
        channels (filter second (db/subscriptions))]
    (doseq [[channel bot-id] channels]
      (if (and event channel bot-id)
        (slackbot/notify-event event channel bot-id)))
    ))

(defn set-notifications []
  (let [events (api/all-events)]
    (doseq [event events]
      (log/info "Setting notifications for event" (:id event))
      (let [notification-time (time/minus5-minutes (:datetime event))]
        (chime-at [notification-time]
                  (fn [_]
                    (notify-subscriptors event)))
        ))))


