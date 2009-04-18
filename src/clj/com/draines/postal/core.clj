(ns com.draines.postal.core
  (:use [clojure.contrib.test-is :only [is deftest run-tests]]
        [com.draines.postal.date :only [make-date]])
  (:import [java.util Date Properties]
           [javax.mail Session Transport Message$RecipientType]
           [javax.mail.internet MimeMessage InternetAddress]
           [com.sun.mail.smtp SMTPSSLTransport]))

;;  Properties props = new Properties();
;;  props.put("mail.smtp.host", "my-mail-server");
;;  props.put("mail.from", "me@example.com");
;;  Session session = Session.getInstance(props, null);
;;
;;  try {
;;      MimeMessage msg = new MimeMessage(session);
;;      msg.setFrom();
;;      msg.setRecipients(Message.RecipientType.TO,
;;                        "you@example.com");
;;      msg.setSubject("JavaMail hello world example");
;;      msg.setSentDate(new Date());
;;      msg.setText("Hello, world!\n");
;;      Transport.send(msg);
;;  } catch (MessagingException mex) {
;;      System.out.println("send failed, exception: " + mex);
;;  }

;; Session.getTransport() and Transport.connect()

(defn get-message [msg]
  (get msg :MimeMessage msg))

(defn get-recipients [msg]
  (map (comp str
             #(.getAddress %))
       (.getAllRecipients (get-message msg))))

(defn add-recipient [msg rtype addr]
  (doto (get-message msg) (.addRecipient rtype (InternetAddress. addr))))

(defn add-recipients [msg rtype addrs]
  (if (string? addrs)
    (add-recipient msg rtype addrs)
    (doseq [addr addrs]
      (add-recipient msg rtype addr)))
  msg)

(defn set-subject [msg subj]
  (.setSubject (get-message msg) subj))

(defn set-body [msg body]
  (.setText (get-message msg) body))

(defn set-date [msg date]
  (.setSentDate (get-message msg) date))

(defn get-subject [msg]
  (.getSubject (get-message msg)))

(defn get-body [msg]
  (.getContent (get-message msg)))

(defn get-date [msg]
  (.getSentDate (get-message msg)))

(defn make-message [attrs]
  (let [{:keys [to cc bcc date subject body]} attrs
        msg (merge attrs {:MimeMessage (MimeMessage. (Session/getInstance (java.util.Properties.)))})]
    (add-recipients msg Message$RecipientType/TO to)
    (add-recipients msg Message$RecipientType/CC cc)
    (add-recipients msg Message$RecipientType/BCC bcc)
    (set-subject msg subject)
    (set-body msg body)
    (set-date msg date)
    msg))

(deftest all
  (let [date (make-date "yyyy-MM-dd" "2009-01-01")
        msg (make-message {:to "Foo Bar <foo@bar.dom>"
                           :cc ["baz@bar.dom" "quux@bar.dom"]
                           :date date
                           :subject "Test"
                           :body "Test!"})]
    (is (= '("foo@bar.dom" "baz@bar.dom" "quux@bar.dom") (get-recipients msg)))
    (is (= "Test" (get-subject msg)))
    (is (= "Test!" (get-body msg)))
    (is (= date (get-date msg)))))

(run-tests)
