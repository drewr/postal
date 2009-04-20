(ns com.draines.postal.smtp
  (:use [com.draines.postal.message :only [make-jmessage]])
  (:import [javax.mail Transport]))

(defn smtp-send [msg]
  (let [jmsg (make-jmessage msg)]
    (Transport/send jmsg)))

