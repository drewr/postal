(ns com.draines.postal.message
  (:use [clojure.contrib.test-is :only [run-tests deftest is]]
        [com.draines.postal.date :only [make-date]])
  (:import [java.util Properties]
           [javax.mail Session Message$RecipientType]
           [javax.mail.internet MimeMessage InternetAddress]))

(declare make-jmessage)

(defn recipients [msg]
  (let [jmsg (make-jmessage msg)]
    (map str (.getAllRecipients jmsg))))

(defn sender [msg]
  (or (:sender msg) (:from msg)))

(defn message->str [msg]
  (let [out (java.io.ByteArrayOutputStream.)]
    (.writeTo (make-jmessage msg) out)
    (str out)))

(defn add-recipient! [jmsg rtype addr]
  (doto jmsg (.addRecipient rtype (InternetAddress. addr))))

(defn add-recipients! [jmsg rtype addrs]
  (if (string? addrs)
    (add-recipient! jmsg rtype addrs)
    (doseq [addr addrs]
      (add-recipient! jmsg rtype addr)))
  jmsg)

(defn make-jmessage [msg]
  (let [{:keys [from to cc bcc date subject body host]} msg
        props (doto (java.util.Properties.)
                (.put "mail.smtp.host" (or host "not.provided"))
                (.put "mail.from" from))
        session (Session/getInstance props)
        jmsg (MimeMessage. (:Session msg))]
    (add-recipients! jmsg Message$RecipientType/TO to)
    (add-recipients! jmsg Message$RecipientType/CC cc)
    (add-recipients! jmsg Message$RecipientType/BCC bcc)
    (.setFrom jmsg (InternetAddress. from))
    (.setSubject jmsg subject)
    (.setText jmsg body)
    (.setSentDate jmsg (or date (make-date)))
    jmsg))

(deftest all
  (let [m {:from "fee@bar.dom"
           :to "Foo Bar <foo@bar.dom>"
           :cc ["baz@bar.dom" "quux@bar.dom"]
           :date (java.util.Date.)
           :subject "Test"
           :body "Test!"}]
    (is (= "Subject: Test" (re-find #"Subject: Test" (message->str m))))))

(comment
  (run-tests))
