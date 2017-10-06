(ns starsconf-slackbot.routes.slack-oauth
  (:require [ring.util.response :refer [redirect]]
            [clj-http.client :as client]
            [starsconf-slackbot.db :as db]
            [starsconf-slackbot.bot.slackbot :as slackbot]
            ))

;;; implementation of https://api.slack.com/docs/oauth

(defn cliend-id [] (or (System/getenv "CLIENT_ID") ""))
(defn client-secret [] (or (System/getenv "CLIENT_SECRET") ""))


(defn oauth-access-request [code]
  (let [url-slack-auth "https://slack.com/api/oauth.access"
        response (client/get url-slack-auth
                             {:as :json
                              :query-params {:client_id     (cliend-id)
                                             :client_secret (client-secret)
                                             :code          code}})]
    response))


(defn store-auth-data! [response]
  (db/add-team (-> response :body :team_id) (:body response)))


(defn slack-oauth-view [code]
  "Sends secret code to slack. If request succeeds, stores credentials in db
   and start a connection to the new team. Then redirects to home page
   with appropiate status code."
  (if-not code
    (redirect "/?error=1")
    (let [response (oauth-access-request code)]
      (if (and (= (:status response) 200) (-> response :body :ok))
        (do (store-auth-data! response)
            (slackbot/new-connection (:body response))
            (redirect "/?installed=1"))
        (redirect "/?error=2"))
      )))


