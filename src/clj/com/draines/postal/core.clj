(ns com.draines.postal.core
  (:use [com.draines.postal.sendmail :only [sendmail-send]]
        [com.draines.postal.smtp :only [smtp-send]]))

(defn send-message [msg]
  (when-not (and (:from msg)
                 (:to msg)
                 (:subject msg)
                 (:body msg))
    (throw (Exception. "message needs at least :from, :to, :subject, and :body")))
  (if (:host ^msg)
    (smtp-send msg)
    (sendmail-send msg)))

