(ns postal.support
  (:import (java.util Properties Random)
           (org.apache.commons.codec.binary Base64)))

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

(defn hostname []
  (.getHostName (java.net.InetAddress/getLocalHost)))

(defn message-id
  ([]
     (message-id (format "postal.%s" (hostname))))
  ([host]
      (let [bs (byte-array 16)
            r (Random.)
            _ (.nextBytes r bs)
            rs (String. (Base64/encodeBase64 bs))
            onlychars (apply str (re-seq #"[0-9A-Za-z]" rs))
            epoch (.getTime (java.util.Date.))]
        (format "%s.%s@%s" onlychars epoch host))))
