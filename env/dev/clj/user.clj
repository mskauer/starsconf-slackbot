(ns user
  (:require 
            [mount.core :as mount]
            starsconf-slackbot.core))

(defn start []
  (mount/start-without #'starsconf-slackbot.core/repl-server))

(defn stop []
  (mount/stop-except #'starsconf-slackbot.core/repl-server))

(defn restart []
  (stop)
  (start))


