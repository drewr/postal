(ns com.draines.postal.core
  (:use [com.draines.postal.sendmail :only [sendmail-send]]
        [com.draines.postal.smtp :only [smtp-send]]))

(defn send-message [msg]
  (if (:host msg)
    (smtp-send msg)
    (sendmail-send msg)))

