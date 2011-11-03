(ns postal.support
  (:import [java.util Properties]))

(defmacro do-when
  [arg condition & body]
  `(when ~condition
     (doto ~arg ~@body)))

(defn make-props [sender {:keys [host port user tls]}]
  (doto (Properties.)
    (.put "mail.smtp.host" (or host "not.provided"))
    (.put "mail.smtp.port" (or port "25"))
    (.put "mail.smtp.auth" (if user "true" "false"))
    (do-when sender (.put "mail.smtp.from" sender))
    (do-when user (.put "mail.smtp.user" user))
    (do-when tls  (.put "mail.smtp.starttls.enable" "true"))))


