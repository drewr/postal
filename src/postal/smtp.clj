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
  (:import [javax.mail Transport Session]))

(defn get-session [server]
  (let [{:keys [sender debug]} server]
    (doto (Session/getInstance (make-props sender server))
      (.setDebug (if debug true false)))))

(defn get-protocol [server]
  (if (:ssl server) "smtps" "smtp"))

(defn get-transport [^Session session ^String protocol]
  (.getTransport session protocol))

(defn connect! [^Transport transport server]
  (let [{:keys [host port user pass]} server]
    (assert (or (and (nil? user) (nil? pass)) (and user pass)))
    (.connect transport host port user pass)))

(defn transport-send
  ([^Transport transport ^Session session msg]
   (transport-send transport (make-jmessage msg session)))
  ([^Transport transport ^javax.mail.Message jmsg]
   (.sendMessage transport jmsg (.getAllRecipients jmsg))
   {:code 0 :error :SUCCESS :message "messages sent"}))

(defn ^:dynamic smtp-send* [^Session session ^String proto
                            {:keys [user pass] :as args} msgs]
  (with-open [^Transport transport (get-transport session proto)]
    (connect! transport args)
    (let [jmsgs (map #(make-jmessage % session) msgs)]
      (doseq [^javax.mail.Message jmsg jmsgs]
        (transport-send transport jmsg))
      {:code 0 :error :SUCCESS :message "messages sent"})))

(defn smtp-send
  ([msg]
     (let [jmsg (make-jmessage msg)]
       (try
         (Transport/send jmsg)
         {:code 0 :error :SUCCESS :message "message sent"}
         (catch Exception e
           {:code 99 :error (class e) :message (.getMessage e)}))))
  ([args & msgs]
     (let [{:keys [host port ssl]
            :or {host "localhost"}} args
            port (if (nil? port)
                   (if ssl 465 25)
                   port)
            proto (get-protocol args)
            args (merge args {:port port
                              :proto proto})
            session (get-session args)]
       (smtp-send* session proto args msgs))))
