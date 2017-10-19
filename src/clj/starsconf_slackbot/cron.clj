(ns starsconf-slackbot.cron
  (:require [chime :refer [chime-at]]
            [starsconf-slackbot.time :as time]
            [starsconf-slackbot.synaptic-api :as api]
            [starsconf-slackbot.db :as db]
            [starsconf-slackbot.bot.slackbot :as slackbot]
            [clojure.tools.logging :as log]
            ))


(defn notify-subscriptors [event]
  (let [channels (filter second (db/subscriptions))]
    (doseq [[channel bot-id] channels]
      (if (and event channel bot-id)
        (slackbot/notify-event event channel bot-id)))
    ))

(defn set-event-notification [event]
  (log/info "Setting notification for event" (:name event)
            "at" (time/pretty-print (:datetime event)))
  (let [notification-time (time/minus5-minutes (:datetime event))]
    (chime-at [notification-time]
              (fn [_]
                (notify-subscriptors event)))))

(defn set-notifications []
  (log/info "Starting notifications...")
  (chime-at (time/every-hour)
            (fn [t]
              (let [time-z (time/to-cl-timezone t)
                    hour+1 (time/plus-1-hour time-z)
                    events (api/get-events-within-next-hour hour+1)]
                (doseq [event events]
                  (set-event-notification event))))))

