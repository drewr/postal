(ns postal.smtp
  (:use [postal.message :only [make-jmessage]]
        [postal.support :only [make-props]])
  (:import [javax.mail Transport Session]))

(defn ^:dynamic smtp-send* [session proto {:keys [host port user pass]} msgs]
  (with-open [transport (.getTransport session proto)]
    (.connect transport host port (str user) (str pass))
    (let [jmsgs (map #(make-jmessage % session) msgs)]
      (doseq [jmsg jmsgs]
        (.sendMessage transport jmsg (.getAllRecipients jmsg)))
      {:code 0 :error :SUCCESS :message "messages sent"})))

(defn smtp-send
  ([msg]
     (let [jmsg (make-jmessage msg)]
       (try
         (Transport/send jmsg)
         {:code 0 :error :SUCCESS :message "message sent"}
         (catch Exception e
           {:code 99 :error (class e) :message (.getMessage e)}))))
  ([args & msgs]
     (let [{:keys [host port user pass sender ssl]
            :or {host "localhost"}} args
           port (if (nil? port)
                  (if ssl 465 25)
                  port)
           proto (if ssl "smtps" "smtp")
           args (merge args {:port port
                             :proto proto})
           session (doto (Session/getInstance (make-props sender args))
                     (.setDebug false))]
       (smtp-send* session proto args msgs))))
