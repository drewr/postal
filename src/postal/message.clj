(ns postal.message
  (:use [clojure.set :only [difference]]
        [clojure.java.io :only [file]]
        [postal.date :only [make-date]]
        [postal.support :only [do-when make-props]])
  (:import [java.util UUID]
           [javax.mail Session Message$RecipientType]
           [javax.mail.internet MimeMessage InternetAddress
            AddressException]
           [javax.mail PasswordAuthentication]))

(def default-charset "utf-8")

(declare make-jmessage)

(defn recipients [msg]
  (let [jmsg (make-jmessage msg)]
    (map str (.getAllRecipients jmsg))))

(defn sender [msg]
  (or (:sender msg) (:from msg)))

(defn make-address
  ([addr charset]
     (let [a (try (InternetAddress. addr)
                  (catch Exception _))]
       (if a
         (InternetAddress. (.getAddress a)
                           (.getPersonal a)
                           charset))))
  ([addr name-str charset]
     (try (InternetAddress. addr name-str charset)
          (catch Exception _))))

(defn make-addresses [addresses charset]
  (if (string? addresses)
    (recur [addresses] charset)
    (into-array InternetAddress (map #(make-address % charset)
                                     addresses))))

(defn message->str [msg]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (let [jmsg (if (instance? MimeMessage msg) msg (make-jmessage msg))]
      (.writeTo jmsg out)
      (str out))))

(defn add-recipient! [jmsg rtype addr charset]
  (if-let [addr (make-address addr charset)]
    (doto jmsg
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
    (let [attachment-part (doto (javax.mail.internet.MimeBodyPart.)
                            (.attachFile (file (:content part)))
                            (.setDisposition (name (:type part))))]

      (when (:content-type part)
        (.setHeader attachment-part "Content-Type" (:content-type part)))
      attachment-part)
    (doto (javax.mail.internet.MimeBodyPart.)
      (.setContent (:content part) (:type part)))))

(defn eval-multipart [parts]
  (let [;; multiparts can have a number of different types: mixed,
        ;; alternative, encrypted...
        ;; The caller can use the first two entries to specify a type.
        ;; If no type is given, we default to "mixed" (for attachments etc.)
        [multiPartType, parts] (if (keyword? (first parts))
                                 [(name (first parts)) (rest parts)]
                                 ["mixed" parts])
        mp (javax.mail.internet.MimeMultipart. multiPartType)]
    (doseq [part parts]
      (.addBodyPart mp (eval-part part)))
    mp))

(defn add-multipart! [jmsg parts]
  (.setContent jmsg (eval-multipart parts)))

(defn add-extra! [jmsg msgrest]
  (doseq [[n v] msgrest]
    (.addHeader jmsg (if (keyword? n) (name n) n) v))
  jmsg)

(defn add-body! [jmsg body charset]
  (if (string? body)
    (doto jmsg (.setText body charset))
    (doto jmsg (add-multipart! body))))

(defn drop-keys [m ks]
  (select-keys m
               (difference (set (keys m)) (set ks))))

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
     (let [standard [:from :reply-to :to :cc :bcc :date :subject :body]
           charset (or (:charset msg) default-charset)
           jmsg (MimeMessage. session)]
       (doto jmsg
         (add-recipients! Message$RecipientType/TO (:to msg) charset)
         (add-recipients! Message$RecipientType/CC (:cc msg) charset)
         (add-recipients! Message$RecipientType/BCC (:bcc msg) charset)
         (.setFrom (if-let [sender (:sender msg)]
                     (make-address sender charset)
                     (make-address (:from msg) charset)))
         (.setReplyTo (when-let [reply-to (:reply-to msg)]
                        (make-addresses reply-to charset)))
         (.setSubject (:subject msg))
         (.setSentDate (or (:date msg) (make-date)))
         (add-extra! (drop-keys msg standard))
         (add-body! (:body msg) charset)))))

(defn make-fixture [from to & {:keys [tag]}]
  (let [uuid (str (UUID/randomUUID))
        tag (or tag "[POSTAL]")]
    {:from from
     :to to
     :subject (format "%s Test -- %s" tag uuid)
     :body (format "Test %s" uuid)}))
