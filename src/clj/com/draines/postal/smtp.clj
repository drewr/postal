(ns com.draines.postal.smtp
  (:use [com.draines.postal.message :only [make-jmessage]])
  (:import [javax.mail Transport]))

(defn smtp-send [msg]
  (let [jmsg (make-jmessage msg)]
    (try
     (Transport/send jmsg)
     {:code 0 :error :SUCCESS :message "message sent"}
     (catch Exception e
       {:code 99 :error (class e) :message (.getMessage e)}))))

