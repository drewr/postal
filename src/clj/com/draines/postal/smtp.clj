(ns com.draines.postal.smtp
  (:use [com.draines.postal.message :only [make-jmessage]])
  (:import [javax.mail Transport Session]))

(defn smtp-send
  ([msg]
     (let [jmsg (make-jmessage msg)]
       (try
         (Transport/send jmsg)
         {:code 0 :error :SUCCESS :message "message sent"}
         (catch Exception e
           {:code 99 :error (class e) :message (.getMessage e)}))))
  ([host port & msgs]
     (let [session (doto (Session/getInstance
                          (doto (java.util.Properties.)
                            (.put "mail.smtp.host" (or host "localhost"))
                            (.put "mail.smtp.port" (or port "25"))))
                     (.setDebug false))]
       (with-open [transport (doto (-> session (.getTransport "smtp"))
                               (.connect "" ""))]
         (let [jmsgs (map #(make-jmessage % session) msgs)]
           (doseq [jmsg jmsgs]
             (.sendMessage transport jmsg (.getAllRecipients jmsg))))))))


