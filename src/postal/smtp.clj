;; Copyright (c) Andrew A. Raines
;;
;; Permission is hereby granted, free of charge, to any person
;; obtaining a copy of this software and associated documentation
;; files (the "Software"), to deal in the Software without
;; restriction, including without limitation the rights to use,
;; copy, modify, merge, publish, distribute, sublicense, and/or sell
;; copies of the Software, and to permit persons to whom the
;; Software is furnished to do so, subject to the following
;; conditions:
;;
;; The above copyright notice and this permission notice shall be
;; included in all copies or substantial portions of the Software.
;;
;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
;; EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
;; OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
;; NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
;; HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
;; WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
;; FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
;; OTHER DEALINGS IN THE SOFTWARE.

(ns postal.smtp
  (:use [postal.message :only [make-jmessage]]
        [postal.support :only [make-props]])
  (:require [clojure.core.async :as a :refer [put!]])
  (:import [javax.mail Transport Session]
           [javax.mail.event TransportListener]))

(defn listener-channel [^Transport transport]
  (let [c (a/chan)
        listener (reify TransportListener
                   (messageDelivered [this e]
                     (put! c {:code 0 :error :SUCCESS :message "messages sent"}))
                   (messageNotDelivered [this e]
                     (put! c {:code 99
                              :error (class e)
                              :message (.getMessage e)}))
                   (messagePartiallyDelivered [this e]
                     (put! c {:code 0 :error :SUCCESS :message "messages sent"})))]
    (.addTransportListener transport listener)
    c))

(defn ^:dynamic smtp-send*
  [^Session session ^String proto {:keys [host port user pass]} msgs]
  (assert (or (and (nil? user) (nil? pass)) (and user pass)))
  (with-open [transport (.getTransport session proto)]
    (.connect transport host port user pass)
    (let [c (listener-channel transport)
          jmsgs (map #(make-jmessage % session) msgs)]
      (a/thread (doseq [^javax.mail.Message jmsg jmsgs]
                  (.sendMessage transport jmsg (.getAllRecipients jmsg))))
      c)))

(defn smtp-send
  ([msg]
     (let [jmsg (make-jmessage msg)]
       (a/thread (try
                   (Transport/send jmsg)
                   {:code 0 :error :SUCCESS :message "message sent"}
                   (catch Exception e
                     {:code 99 :error (class e) :message (.getMessage e)})))))
  ([args & msgs]
     (let [{:keys [host port user pass sender ssl]
            :or {host "localhost"}} args
            port (if (nil? port)
                   (if ssl 465 25)
                   port)
            proto (if ssl "smtps" "smtp")
            args (merge args {:port port
                              :proto proto})
            session (doto (Session/getInstance (make-props sender args))
                      (.setDebug false))]
       (smtp-send* session proto args msgs))))
