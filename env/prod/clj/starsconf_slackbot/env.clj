(ns starsconf-slackbot.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[starsconf-slackbot started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[starsconf-slackbot has shut down successfully]=-"))
   :middleware identity})
