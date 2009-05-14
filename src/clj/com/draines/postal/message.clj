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
  (let [out (java.io.ByteArrayOutputStream.)
        jmsg (if (instance? MimeMessage msg) msg (make-jmessage msg))]
    (.writeTo jmsg out)
    (str out)))

(defn add-recipient! [jmsg rtype addr]
  (doto jmsg (.addRecipient rtype (InternetAddress. addr))))

(defn add-recipients! [jmsg rtype addrs]
  (if (string? addrs)
    (add-recipient! jmsg rtype addrs)
    (doseq [addr addrs]
      (add-recipient! jmsg rtype addr)))
  jmsg)

(defn add-multipart! [jmsg parts]
  (let [mp (javax.mail.internet.MimeMultipart.)
        fileize (fn [x]
                  (if (instance? java.io.File x) x (java.io.File. x)))]
    (doseq [part parts]
      (condp (fn [test type] (some #(= % type) test)) (:type part)
        [:inline :attachment] (.addBodyPart mp
                                            (doto (javax.mail.internet.MimeBodyPart.)
                                              (.attachFile (fileize (:content part)))
                                              (.setDisposition (name (:type part)))))
        (.addBodyPart mp
                      (doto (javax.mail.internet.MimeBodyPart.)
                        (.setContent (:content part) (:type part))))))
    (.setContent jmsg mp)))

(defn add-body! [jmsg body]
  (if (string? body)
    (doto jmsg (.setText body))
    (doto jmsg (add-multipart! body))))

(defn make-jmessage
  ([msg]
     (let [{:keys [sender from]} msg
           {:keys [host port]} ^msg
           props (doto (java.util.Properties.)
                   (.put "mail.smtp.host" (or host "not.provided"))
                   (.put "mail.smtp.port" (or port "25"))
                   (.put "mail.smtp.from" (or sender from)))
           session (or (:session ^msg) (Session/getInstance props))]
       (make-jmessage msg session)))
  ([msg session]
     (let [{:keys [from to cc bcc date subject body]} msg
           jmsg (MimeMessage. session)]
       (doto jmsg
         (add-recipients! Message$RecipientType/TO to)
         (add-recipients! Message$RecipientType/CC cc)
         (add-recipients! Message$RecipientType/BCC bcc)
         (.setFrom (InternetAddress. from))
         (.setSubject subject)
         (.setSentDate (or date (make-date)))
         (add-body! body)))))

(deftest test-simple
  (let [m {:from "fee@bar.dom"
           :to "Foo Bar <foo@bar.dom>"
           :cc ["baz@bar.dom" "quux@bar.dom"]
           :date (java.util.Date.)
           :subject "Test"
           :body "Test!"}]
    (is (= "Subject: Test" (re-find #"Subject: Test" (message->str m))))))

(deftest test-multipart
  (let [m {:from "foo@bar.dom"
           :to "baz@bar.dom"
           :subject "Test"
           :body [{:type "text/html"
                   :content "<b>some html</b>"}]}]
    (is (= "multipart/mixed" (re-find #"multipart/mixed" (message->str m))))
    (is (= "Content-Type: text/html" (re-find #"Content-Type: text/html" (message->str m))))
    (is (= "some html" (re-find #"some html" (message->str m))))))

(deftest test-inline
  (let [f (doto (java.io.File/createTempFile "_postal-" ".txt"))
        _ (doto (java.io.PrintWriter. f) (.println "tempfile contents") (.close))
        m {:from "foo@bar.dom"
           :to "baz@bar.dom"
           :subject "Test"
           :body [{:type :inline
                   :content f}]}]
    (is (= "tempfile" (re-find #"tempfile" (message->str m))))
    (.delete f)))

(deftest test-attachment
  (let [f1 (doto (java.io.File/createTempFile "_postal-" ".txt"))
        _ (doto (java.io.PrintWriter. f1) (.println "tempfile contents") (.close))
        f2 "/etc/resolv.conf"
        m {:from "foo@bar.dom"
           :to "baz@bar.dom"
           :subject "Test"
           :body [{:type :attachment
                   :content f1}
                  {:type :attachment
                   :content f2}]}]
    (is (= "tempfile" (re-find #"tempfile" (message->str m))))
    (.delete f1)))

(comment
  (run-tests))
