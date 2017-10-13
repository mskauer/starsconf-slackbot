(ns starsconf-slackbot.bot.ai
  (:require [clj-http.client :as client]
            [superstring.core :as str]
            [clojure.tools.logging :as log]
            [starsconf-slackbot.synaptic-api :as api]
            [starsconf-slackbot.time :as time]
            [starsconf-slackbot.db :as db]
            ))

;;; not really any ai :)

(defn help-message [bot-id]
  (str "prueba escribiendo uno de los siguientes mensajes:\n"
       "        " bot-id " proxima\n"
       "        " bot-id " suscribirse\n"
       "        " bot-id " cancelar\n"
       "        " bot-id " ayuda\n"
       ))

(defn default-message [bot-id]
  (help-message bot-id))

(defn event-to-weekday [e]
  (let [date (-> e :timeSlot :date)]
    ({"2017-11-03" "Viernes"
      "2017-11-04" "Sábado"} date "-")))


(defn start-time [e]
  (let [time (-> e :timeSlot :start)]
    (str/chop-suffix time ":00")))


(defn end-time [e]
  (let [time (-> e :timeSlot :end)]
    (str/chop-suffix time ":00")))


(defn parse-real-event
  "Real event meaning not placeholder"
  [e]
  (str "charla " (:name e) ",de " (:speaker e "-") "\n"
       "Día " (event-to-weekday e) ", entre " (start-time e) " y " (end-time e) " hrs.\n"
       "Lugar: sala " (:room e "-") " - Categoría: " (:category e "-") "\n"
       ))


(defn parse-placeholder-event [e]
  (str (:name e) "\n"
       "Día " (event-to-weekday e) ", entre " (start-time e) " y " (end-time e) " hrs.\n"
       "Lugar: sala  " (:room e "-") "\n"
       ))

(defn parse-event [e]
  (if (:isPlaceholder e)
    (parse-placeholder-event e)
    (parse-real-event e)))


(defn next-event-message []
  (let [now (time/now)
        events (api/next-n-events-from 2 now)]
    (if (:isPlaceholder (first events))
      (str "Ahora viene: " (parse-event (first events))
           "Y luego: "     (parse-event (second events)))
      (str "Ahora viene: " (parse-event (first events)))
      )))


(defn subscription-message [bot-id channel]
  (db/add-subscription channel (str/chop-suffix (str/chop-prefix bot-id "<@") ">"))
  (str "Has suscrito este canal a las notificaciones. Se te avisará 5 minutos antes de cada charla. Puedes cancelar escribiéndome:\n"
       bot-id " cancelar"))

(defn cancel-subscription-message [channel]
  (db/cancel-subscription channel)
  (str "Has cancelado la suscripción. Ya no recibirás mensajes automáticos en este canal."))


(defn categories-message []
  (str "Las categorias de las charlas son: " (str/join ", " (api/all-categories))))


(defn clean-msg [msg bot-id]
  (-> msg
      (str/trim)
      (str/chop-prefix bot-id)
      (str/strip-accents)
      (str/lower-case)
      (str/trim)
      (str/replace #"[^a-zA-Z ]" "")
      ))


(defn handle-msg-request [msg bot-id channel]
  (let [tokens (str/split (clean-msg msg bot-id) #" +")
        command (first tokens)]
    (cond
      (= command "proximo") (next-event-message)
      (= command "proxima") (next-event-message)
      (= command "suscribirse") (subscription-message bot-id channel)
      (= command "cancelar") (cancel-subscription-message channel)
      (= command "categorias") (categories-message)
      :else (default-message bot-id)
      )))


(defn response-text
  "Only reply to messages that start with @<bot-id>"
  [msg sender-id bot-id channel]
  (if (str/starts-with? (str/trim msg) bot-id)
    (str sender-id ", " (handle-msg-request msg bot-id channel))))

