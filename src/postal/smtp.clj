(ns postal.smtp
  (:use [postal.message :only [make-jmessage]]
        [postal.support :only [make-props]])
  (:import [javax.mail Transport Session]))

(defn smtp-send
  ([msg]
     (let [jmsg (make-jmessage msg)]
       (try
         (Transport/send jmsg)
         {:code 0 :error :SUCCESS :message "message sent"}
         (catch Exception e
           {:code 99 :error (class e) :message (.getMessage e)}))))
  ([auth-map & msgs]
     (let [{:keys [host port
                   user pass
                   sender ssl] :or {host "localhost"}}
           auth-map
           port (if (not port)
                  (if ssl 465 25)
                  port)
           protocol (if ssl "smtps" "smtp")
           session (doto (Session/getInstance (make-props sender auth-map))
                     (.setDebug false))]
       (with-open [transport (.getTransport session (if ssl "smtps" "smtp"))]
         (.connect transport host port (str user) (str pass))
         (let [jmsgs (map #(make-jmessage % session) msgs)]
           (doseq [jmsg jmsgs]
             (.sendMessage transport jmsg (.getAllRecipients jmsg)))
           {:code 0 :error :SUCCESS :message "messages sent"})))))
