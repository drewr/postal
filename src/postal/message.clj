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

(ns postal.message
  (:use [clojure.set :only [difference]]
        [clojure.java.io :only [file]]
        [postal.date :only [make-date]]
        [postal.support :only [do-when make-props message-id user-agent]])
  (:import [java.util UUID]
           [javax.mail Session Message$RecipientType]
           [javax.mail.internet MimeMessage InternetAddress
            AddressException]
           [javax.mail PasswordAuthentication]))

(set! *warn-on-reflection* true)

(def default-charset "utf-8")

(def ^{:dynamic true :private true} *charset*)

(declare make-jmessage)

(defn recipients [msg]
  (let [^javax.mail.Message jmsg (make-jmessage msg)]
    (map str (.getAllRecipients jmsg))))

(defn sender [msg]
  (or (:sender msg) (:from msg)))

(defn make-address
  ([addr]
     (make-address addr nil))
  ([addr name-str]
     (try
       (let [iaddr (InternetAddress. addr)
             name-str (or name-str (.getPersonal iaddr))]
         ;; ensure charset is set correctly for personal name
         (.setPersonal iaddr name-str *charset*)
         iaddr)
       (catch Exception _))))

(defn make-addresses [addresses]
  (if (string? addresses)
    (recur [addresses])
    (into-array InternetAddress (map make-address addresses))))

(defn message->str [msg]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (let [^javax.mail.Message jmsg (if (instance? MimeMessage msg)
                                     msg (make-jmessage msg))]
      (.writeTo jmsg out)
      (str out))))

(defn add-recipient! [jmsg rtype addr]
  (if-let [addr (make-address addr)]
    (.addRecipient ^javax.mail.Message jmsg rtype addr)))

(defn add-recipients! [jmsg rtype addrs]
  (if (string? addrs)
    (add-recipient! jmsg rtype addrs)
    (doseq [addr addrs]
      (add-recipient! jmsg rtype addr))))

(defn- ^java.io.File fileize [x]
  (if (instance? java.io.File x) x (java.io.File. ^String x)))

(declare eval-bodypart eval-multipart)

(defprotocol PartEval (eval-part [part]))

(extend-protocol PartEval
  clojure.lang.IPersistentMap
  (eval-part [part] (eval-bodypart part))
  clojure.lang.IPersistentCollection
  (eval-part [part]
    (doto (javax.mail.internet.MimeBodyPart.)
      (.setContent (eval-multipart part)))))

(defn eval-bodypart [part]
  (condp (fn [test type] (some #(= % type) test)) (:type part)
    [:inline :attachment]
    (let [attachment-part (doto (javax.mail.internet.MimeBodyPart.)
                            (.attachFile (file (:content part)))
                            (.setDisposition (name (:type part))))]

      (when (:content-type part)
        (.setHeader attachment-part "Content-Type" (:content-type part)))
      (when (:content-id part)
        (.setContentID attachment-part (str "<" (:content-id part) ">")))
      (when (:file-name part)
        (.setFileName attachment-part (:file-name part)))
      (when (:description part)
        (.setDescription attachment-part (:description part)))
      attachment-part)
    (doto (javax.mail.internet.MimeBodyPart.)
      (.setContent (:content part) (:type part)))))

(defn eval-multipart [parts]
  (let [;; multiparts can have a number of different types: mixed,
        ;; alternative, encrypted...
        ;; The caller can use the first two entries to specify a type.
        ;; If no type is given, we default to "mixed" (for attachments etc.)
        [^String multiPartType, parts] (if (keyword? (first parts))
                                 [(name (first parts)) (rest parts)]
                                 ["mixed" parts])
        mp (javax.mail.internet.MimeMultipart. multiPartType)]
    (doseq [part parts]
      (.addBodyPart mp (eval-part part)))
    mp))

(defn add-multipart! [^javax.mail.Message jmsg parts]
  (.setContent jmsg (eval-multipart parts)))

(defn add-extra! [^javax.mail.Message jmsg msgrest]
  (doseq [[n v] msgrest]
    (.addHeader jmsg (if (keyword? n) (name n) n) v)))

(defn add-body! [jmsg body]
  (if (string? body)
    (.setText ^MimeMessage jmsg body *charset*)
    (add-multipart! jmsg body)))

(defn drop-keys [m ks]
  (select-keys m (difference (set (keys m)) (set ks))))

(defn make-auth [user pass]
  (proxy [javax.mail.Authenticator] []
    (getPasswordAuthentication [] (PasswordAuthentication. user pass))))

(defn make-jmessage
  ([msg]
     (let [{:keys [sender from]} msg
           {:keys [user pass]} (meta msg)
           props (make-props (or sender from) (meta msg))
           session (or (:session (meta msg))
                       (if user
                         (Session/getInstance props (make-auth user pass))
                         (Session/getInstance props)))]
       (make-jmessage msg session)))
  ([msg session]
     (let [standard [:from :reply-to :to :cc :bcc
                     :date :subject :body :message-id
                     :user-agent :charset]
           jmsg (proxy [MimeMessage] [^Session session]
                  (updateMessageID []
                    (.setHeader
                     ^MimeMessage this
                     "Message-ID" ((:message-id msg message-id)))))]
       (binding [*charset* (or (:charset msg) default-charset)]
         (doto jmsg
           (add-recipients! Message$RecipientType/TO (:to msg))
           (add-recipients! Message$RecipientType/CC (:cc msg))
           (add-recipients! Message$RecipientType/BCC (:bcc msg))
           (.setFrom (make-address (:from msg)))
           (.setReplyTo (when-let [reply-to (:reply-to msg)]
                          (make-addresses reply-to)))
           (.setSubject (:subject msg))
           (.setSentDate (or (:date msg) (make-date)))
           (.addHeader "User-Agent" (:user-agent msg (user-agent)))
           (add-extra! (drop-keys msg standard))
           (add-body! (:body msg))
           (.saveChanges))))))

(defn make-fixture [from to & {:keys [tag]}]
  (let [uuid (str (UUID/randomUUID))
        tag (or tag "[POSTAL]")]
    {:from from
     :to to
     :subject (format "%s Test -- %s" tag uuid)
     :body (format "Test %s" uuid)}))
