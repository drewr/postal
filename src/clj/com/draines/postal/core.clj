(ns com.draines.postal.core
  (:use [clojure.contrib.test-is :only [is deftest run-tests]]
        [com.draines.postal.date :only [make-date]])
  (:import [java.util Date Properties]
           [javax.mail Session Transport Message$RecipientType]
           [javax.mail.internet MimeMessage InternetAddress]))

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

(defn recipients [msg]
  (let [jmsg (make-jmessage msg)]
    (map str (.getAllRecipients jmsg))))

(defn sender [msg]
  (or (:sender msg) (:from msg)))

(defn message->str [msg]
  (let [out (java.io.ByteArrayOutputStream.)]
    (.writeTo (make-jmessage msg) out)
    (str out)))

(defn smtp-send [msg]
  (let [jmsg (make-jmessage msg)]
    (Transport/send jmsg)))

(defn sendmail-send [msg]
  (let [mail (message->str msg)
        cmd (concat
             ["/usr/sbin/sendmail" (format "-f %s" (sender msg))]
             (recipients msg))
        pb (ProcessBuilder. cmd)
        p (.start pb)
        smtp (java.io.PrintStream. (.getOutputStream p))]
    (.print smtp mail)
    (.close smtp)
    (.waitFor p)
    (condp = (.exitValue p)
      0 :success
      [:error (.exitValue p)])))

(defn send-message [msg]
  (if (:host msg)
    (smtp-send msg)
    (sendmail-send msg)))

(comment
  (send-message {:from "fee@bar.dom"
                 :to "Foo Bar <foo@bar.dom>"
                 :cc ["baz@bar.dom" "quux@bar.dom"]
                 :date (make-date "yyyy-MM-dd" "2009-01-01")
                 :subject "Test"
                 :body "Test!"})

)
