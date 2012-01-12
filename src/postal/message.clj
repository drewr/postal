(ns postal.message
  (:use [clojure.set :only [difference]]
        [postal.date :only [make-date]]
        [postal.support :only [do-when make-props]])
  (:import [java.util UUID]
           [javax.mail Session Message$RecipientType]
           [javax.mail.internet MimeMessage InternetAddress
            AddressException]
           [javax.mail PasswordAuthentication]))

(declare make-jmessage)

(defn recipients [msg]
  (let [jmsg (make-jmessage msg)]
    (map str (.getAllRecipients jmsg))))

(defn sender [msg]
  (or (:sender msg) (:from msg)))

(defn make-address
  ([addr]
     (try (InternetAddress. addr)
          (catch Exception _)))
  ([addr name-str]
     (try (InternetAddress. addr name-str)
          (catch Exception _))))

(defn make-addresses [addresses]
  (if (string? addresses)
    (recur [addresses])
    (into-array InternetAddress (map make-address addresses))))

(defn message->str [msg]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (let [jmsg (if (instance? MimeMessage msg) msg (make-jmessage msg))]
      (.writeTo jmsg out)
      (str out))))

(defn add-recipient! [jmsg rtype addr]
  (if-let [addr (make-address addr)]
    (doto jmsg
      (.addRecipient rtype addr))
    jmsg))

(defn add-recipients! [jmsg rtype addrs]
  (when addrs
    (if (string? addrs)
      (add-recipient! jmsg rtype addrs)
      (doseq [addr addrs]
        (add-recipient! jmsg rtype addr))))
  jmsg)

(defn add-multipart! [jmsg parts]
  (let [;; multiparts can have a number of different types: mixed,
        ;; alternative, encrypted...
        ;; The caller can use the first two entries to specify a type.
        ;; If no type is given, we default to "mixed" (for attachments etc.)
        [multiPartType, parts] (if (keyword? (first parts))
                                 [(name (first parts)) (rest parts)]
                                 ["mixed" parts])
        mp (javax.mail.internet.MimeMultipart. multiPartType)
        fileize (fn [x]
                  (if (instance? java.io.File x) x (java.io.File. x)))]
    (doseq [part parts]
      (condp (fn [test type] (some #(= % type) test)) (:type part)
        [:inline :attachment]
        (.addBodyPart mp
                      (doto (javax.mail.internet.MimeBodyPart.)
                        (.attachFile (fileize (:content part)))
                        (.setDisposition (name (:type part)))))
        (.addBodyPart mp
                      (doto (javax.mail.internet.MimeBodyPart.)
                        (.setContent (:content part) (:type part))))))
    (.setContent jmsg mp)))

(defn add-extra! [jmsg msgrest]
  (doseq [[n v] msgrest]
    (.addHeader jmsg (if (keyword? n) (name n) n) v))
  jmsg)

(defn add-body! [jmsg body]
  (if (string? body)
    (doto jmsg (.setText body))
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
           jmsg (MimeMessage. session)]
       (doto jmsg
         (add-recipients! Message$RecipientType/TO (:to msg))
         (add-recipients! Message$RecipientType/CC (:cc msg))
         (add-recipients! Message$RecipientType/BCC (:bcc msg))
         (.setFrom (if-let [sender (:sender msg)]
                     (make-address (:from msg) sender)
                     (make-address (:from msg))))
         (.setReplyTo (when-let [reply-to (:reply-to msg)]
                        (make-addresses reply-to)))
         (.setSubject (:subject msg))
         (.setSentDate (or (:date msg) (make-date)))
         (add-extra! (drop-keys msg standard))
         (add-body! (:body msg))))))

(defn make-fixture [from to & {:keys [tag]}]
  (let [uuid (str (UUID/randomUUID))
        tag (or tag "[POSTAL]")]
    {:from from
     :to to
     :subject (format "%s Test -- %s" tag uuid)
     :body (format "Test %s" uuid)}))
