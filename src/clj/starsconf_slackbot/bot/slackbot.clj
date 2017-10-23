(ns starsconf-slackbot.bot.slackbot
  (:require [slack-rtm.core :as slack]
            [starsconf-slackbot.db :as db]
            [clojure.tools.logging :as log]
            [starsconf-slackbot.bot.ai :as ai]
            [clj-http.client :as client]
            ))

(defonce rtm-connections (atom {}))


(defn slack-id-to-msg
  "Slack's notation to identify ids in chat. E.g. 'D7ALHP8AG' -> '<@D7ALHP8AG>' "
  [user-id]
  (str "<@" user-id ">"))


(defn bot-id-by-team [team-id]
  (let [id (db/bot-id team-id)]
    (slack-id-to-msg id)))


(defn should-reply-to-event
  "Ignore events that are not simple messages."
  [event]
  (and (= (:type event) "message")
       (:user event)
       (not (:subtype event))
       (not (:reply_to event))
       ))

(defn notify-event [event channel bot-id]
  (let [team-id (db/team-id bot-id)
        dispatcher (:dispatcher (@rtm-connections team-id))]
    (slack/send-event dispatcher
                      {:type :message
                       :channel channel
                       :text (str "Próximo evento: " (ai/parse-event event))})))

(defn notify-new-channel [channel bot-id]
  (let [team-id (db/team-id bot-id)
        dispatcher (:dispatcher (@rtm-connections team-id))
        msg (str "Has instalado StarsConf2017Bot. Para recibir notificaciones automáticamente escribe: " (slack-id-to-msg bot-id) " suscribirse")]
    (slack/send-event dispatcher
                      {:type :message
                       :channel channel
                       :text msg})))


(defn msg-receiver [dispatcher event team-id]
  (if (should-reply-to-event event)
    (let [channel (:channel event)
          message (:text event)
          sender-id (slack-id-to-msg (:user event))
          bot-id (bot-id-by-team team-id)]
      (if-let [response-text (ai/response-text message sender-id bot-id channel)]
        (slack/send-event dispatcher
                          {:type :message
                           :channel channel
                           :text response-text}
                          )))))


(defn start-connections []
  (log/info "Starting slack rtm websocket connections...")
  (let [teams (db/teams)
        conns (into {}
                    (for [[team-id team] teams
                          :when (-> team :bot :bot_access_token)]
                      [team-id (slack/connect (-> team :bot :bot_access_token))]))]
    (reset! rtm-connections conns)
    (doseq [[team-id conn] conns]
      (slack/sub-to-event (:events-publication conn)
                          :message
                          #(msg-receiver (:dispatcher conn) % team-id)))
    (log/info "Started" (count conns) "connections.")
    ))


(defn close-connections []
  (log/info "Closing slack rtm connections...")
  (doseq [[team-id conn] @rtm-connections]
    (log/info "Closing connection:" team-id)
    (slack/send-event (:dispatcher conn) :close)))


(defn get-app-channels [bot-token bot-id]
  (let [url "https://slack.com/api/im.list"
        response (client/get url {:as :json :query-params {:token bot-token}})]
    (if (and (= (:status response) 200) (-> response :body :ok))
      (filter #(not= (:user %) "USLACKBOT") (-> response :body :ims))
      )))


(defn new-connection [team]
  (log/info "New connection:" (:team_id team))
  (if-not (contains? @rtm-connections (:team_id team))
    (let [connection (slack/connect (-> team :bot :bot_access_token))
          msg-handler #(msg-receiver (:dispatcher connection) % (:team_id team))]
      (slack/sub-to-event (:events-publication connection)
                          :message
                          msg-handler)
      (swap! rtm-connections assoc (:team_id team) connection))
    (log/info "Team already connected."))
  (let [channels (get-app-channels
                  (-> team :bot :bot_access_token)
                  (-> team :bot :bot_user_id))]
    (doseq [channel channels]
      (notify-new-channel (:id channel) (-> team :bot :bot_user_id)))))


