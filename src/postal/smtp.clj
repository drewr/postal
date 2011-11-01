(ns postal.smtp
  (:use [postal.message :only [make-jmessage]])
  (:import [javax.mail Transport Session]))

(defn smtp-send
  ([msg]
     (let [jmsg (make-jmessage msg)]
       (try (Transport/send jmsg)
            {:code 0 :error :SUCCESS :message "message sent"}
            (catch Exception e
              {:code 99 :error (class e) :message (.getMessage e)}))))
  ([auth-map & msgs]
     (let [{:keys [host port user pass] :or {host "localhost"
                                             port 25}}
           auth-map
           session (doto (Session/getInstance
                          (doto (java.util.Properties.)
                            (.put "mail.smtp.host" (or host "localhost"))
                            (.put "mail.smtp.port" (str (or port 25)))))
                     (.setDebug false))]
       (with-open [transport (.getTransport session (if (and user pass)
                                                      "smtps"
                                                      "smtp"))]
         (.connect transport host port (str user) (str pass))
         (let [jmsgs (map #(make-jmessage % session) msgs)]  
           (doseq [jmsg jmsgs]
             (.sendMessage transport jmsg (.getAllRecipients jmsg))))))))


