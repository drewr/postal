(ns postal.core
  (:use [postal.sendmail :only [sendmail-send]]
        [postal.smtp :only [smtp-send]]
        [postal.stress :only [spam]]))

(defn send-message
  ([{:keys [host] :as server}
    {:keys [from to subject body] :or {to "" subject ""} :as msg}]
     (when-not (and from to)
       (throw (Exception. "message needs at least :from and :to")))
     (if host
       (smtp-send server msg)
       (sendmail-send msg)))
  ([msg]
     (send-message (meta msg) msg)))

(defn stress [profile]
  (let [defaults #^{:host "localhost"
                    :port 25
                    :num 1
                    :delay 100
                    :threads 1}
        {:from "foo@lolz.dom"
         :to "bar@lolz.dom"}
        {:keys [host port from to num delay threads]}
        (merge (meta defaults) defaults (meta profile) profile)]
    (println (format "sent %s msgs to %s:%s"
                     (spam host port from to num delay threads)
                     host port))))
