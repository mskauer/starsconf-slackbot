(ns starsconf-slackbot.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [starsconf-slackbot.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[starsconf-slackbot started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[starsconf-slackbot has shut down successfully]=-"))
   :middleware wrap-dev})
