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
        [clojure.java.io :only [as-url as-file]]
        [postal.date :only [make-date]]
        [postal.support :only [do-when make-props message-id user-agent]])
  (:import [java.util UUID]
           [java.net MalformedURLException]
           [javax.activation DataHandler]
           [javax.mail Session Message$RecipientType]
           [javax.mail.internet MimeMessage InternetAddress
            AddressException]
           [javax.mail PasswordAuthentication]))

(def default-charset "utf-8")

(declare make-jmessage)

(defn recipients [msg]
  (let [^javax.mail.Message jmsg (make-jmessage msg)]
    (map str (.getAllRecipients jmsg))))

(defn sender [msg]
  (or (:sender msg) (:from msg)))

(defn make-address
  ([^String addr ^String charset]
   (let [a (try (InternetAddress. addr)
                (catch Exception _))]
     (if a
       (InternetAddress. (.getAddress a)
                         (.getPersonal a)
                         charset))))
  ([^String addr ^String name-str ^String charset]
   (try (InternetAddress. addr name-str charset)
        (catch Exception _))))

(defn make-addresses [addresses charset]
  (if (string? addresses)
    (recur [addresses] charset)
    (into-array InternetAddress (map #(make-address % charset)
                                     addresses))))

(defn- make-url [x]
  (try (as-url x)
       (catch MalformedURLException e
         (as-url (as-file x)))))

(defn message->str [msg]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (let [^javax.mail.Message jmsg (if (instance? MimeMessage msg)
                                     msg (make-jmessage msg))]
      (.writeTo jmsg out)
      (str out))))

(defn add-recipient! [jmsg rtype addr charset]
  (if-let [addr (make-address addr charset)]
    (doto ^javax.mail.Message jmsg
      (.addRecipient rtype addr))
    jmsg))

(defn add-recipients! [jmsg rtype addrs charset]
  (when addrs
    (if (string? addrs)
      (add-recipient! jmsg rtype addrs charset)
      (doseq [addr addrs]
        (add-recipient! jmsg rtype addr charset))))
  jmsg)

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
    (let [url (make-url (:content part))]
      (doto (javax.mail.internet.MimeBodyPart.)
        (.setDataHandler (DataHandler. url))
        (.setFileName (:file-name part
                                  (.toUpperCase
                                   (str (java.util.UUID/randomUUID)))))
        (.setDisposition (name (:type part)))
        (cond-> (:content-type part)
                (.setHeader "Content-Type" (:content-type part)))
        (cond-> (:content-id part)
                (.setContentID (str "<" (:content-id part) ">")))
        (cond-> (:file-name part)
                (.setFileName (:file-name part)))
        (cond-> (:description part)
                (.setDescription (:description part)))))
    (doto (javax.mail.internet.MimeBodyPart.)
      (.setContent (:content part) (:type part))
      (cond-> (:file-name part)
              (.setFileName (:file-name part)))
      (cond-> (:description part)
              (.setDescription (:description part))))))

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
    (.addHeader jmsg (if (keyword? n) (name n) n) v))
  jmsg)

(defn add-body! [^javax.mail.Message jmsg body charset]
  (if (string? body)
    (doto jmsg (.setText body charset))
    (doto jmsg (add-multipart! body))))

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
                   :user-agent]
         charset (or (:charset msg) default-charset)
         jmsg (proxy [MimeMessage] [session]
                (updateMessageID []
                  (.setHeader
                   this
                   "Message-ID" ((:message-id msg message-id)))))]
     (doto jmsg
       (add-recipients! Message$RecipientType/TO (:to msg) charset)
       (add-recipients! Message$RecipientType/CC (:cc msg) charset)
       (add-recipients! Message$RecipientType/BCC (:bcc msg) charset)
       (.setFrom (if-let [sender (:sender msg)]
                   (make-address sender charset)
                   (make-address (:from msg) charset)))
       (.setReplyTo (when-let [reply-to (:reply-to msg)]
                      (make-addresses reply-to charset)))
       (.setSubject (:subject msg) charset)
       (.setSentDate (or (:date msg) (make-date)))
       (.addHeader "User-Agent" (:user-agent msg (user-agent)))
       (add-extra! (apply dissoc msg standard))
       (add-body! (:body msg) charset)
       (.saveChanges)))))

(defn make-fixture [from to & {:keys [tag]}]
  (let [uuid (str (UUID/randomUUID))
        tag (or tag "[POSTAL]")]
    {:from from
     :to to
     :subject (format "%s Test -- %s" tag uuid)
     :body (format "Test %s" uuid)}))
