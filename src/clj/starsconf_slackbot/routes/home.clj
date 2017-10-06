(ns starsconf-slackbot.routes.home
  (:require [starsconf-slackbot.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [starsconf-slackbot.routes.slack-oauth :as slack-routes]))


(defn home-page [query-params]
  (layout/render
    "home.html" query-params))


(defroutes home-routes
  (GET "/" {:keys [query-params]} (home-page query-params))
  (GET "/slack-oauth" [code] (slack-routes/slack-oauth-view code)))

