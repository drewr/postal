(ns postal.test.message
  (:use [postal.message]
        [clojure.test :only [run-tests deftest is]]
        [postal.date :only [make-date]])
  (:import [java.util Properties UUID]
           [javax.mail Session Message$RecipientType]
           [javax.mail.internet MimeMessage InternetAddress
            AddressException]))

(deftest test-simple
  (let [m (message->str
           {:from "fee@bar.dom"
            :to "Foo Bar <foo@bar.dom>"
            :cc ["baz@bar.dom" "Quux <quux@bar.dom>"]
            :date (java.util.Date.)
            :subject "Test"
            :body "Test!"})]
    (is (.contains m "Subject: Test"))
    (is (.contains m "Cc: baz@bar.dom, Quux <quux@bar.dom>"))))

(deftest test-multipart
  (let [m (message->str
           {:from "foo@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body [{:type "text/html"
                    :content "<b>some html</b>"}]})]
    (is (.contains m "multipart/mixed"))
    (is (.contains m "Content-Type: text/html"))
    (is (.contains m "some html"))))

(deftest test-inline
  (let [f (doto (java.io.File/createTempFile "_postal-" ".txt"))
        _ (doto (java.io.PrintWriter. f)
            (.println "tempfile contents") (.close))
        m (message->str
           {:from "foo@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body [{:type :inline
                    :content f}]})]
    (is (.contains m "tempfile"))
    (.delete f)))

(deftest test-attachment
  (let [f1 (doto (java.io.File/createTempFile "_postal-" ".txt"))
        _ (doto (java.io.PrintWriter. f1)
            (.println "tempfile contents") (.close))
        f2 "/etc/resolv.conf"
        m (message->str
           {:from "foo@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body [{:type :attachment
                    :content f1}
                   {:type :attachment
                    :content f2}]})]
    (is (.contains m "tempfile"))
    (.delete f1)))

(deftest test-nested
  (let [f (doto (java.io.File/createTempFile "_postal-" ".txt"))
        _ (doto (java.io.PrintWriter. f)
            (.println "tempfile contents") (.close))
        m (message->str
           {:from "foo@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body [[:alternative
                    {:type "text/html"
                     :content "<b>some html</b>"}
                    {:type "text/plain"
                     :content "some text"}]
                   {:type :attachment
                    :content f}]})]
    (is (.contains m "multipart/mixed"))
    (is (.contains m "multipart/alternative"))
    (is (.contains m "Content-Type: text/html"))
    (is (.contains m "some html"))
    (is (.contains m "Content-Type: text/plain"))
    (is (.contains m "some text"))
    (is (.contains m "tempfile"))
    (.delete f)))

(deftest test-fixture
  (let [from "foo@bar.dom"
        to "baz@bar.dom"
        tag "[TEST]"]
    (is (zero? (.indexOf (:subject (make-fixture from to :tag tag)) "[TEST")))))

(deftest test-extra-headers
  (let [m {:from "foo@bar.dom"
           :to "baz@bar.dom"
           :subject "Test"
           :User-Agent "Lorem Ipsum"
           :body "Foo!"}]
    (is (.contains (message->str m) "User-Agent: Lorem Ipsum"))))

(deftest test-bad-addrs
  (let [m (message->str
           {:from "foo @bar.dom"
            :to "badddz@@@bar.dom"
            :subject "Test"
            :body "Bad recipient!"})]
    (is (not (.contains m "badddz")))
    (is (not (.contains m "foo @bar")))))

(deftest test-reply-to
  (let [m (message->str
           {:from "foo@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body "Reply me!"
            :reply-to "yermom@bar.dom"})]
    (is (.contains m "Reply-To: yermom"))))

(deftest test-only-bcc
  (let [m (message->str
           {:from "foo@bar.dom"
            :bcc "baz@bar.dom"
            :subject "Test"
            :body "Only Bcc!!"})]
    (is (.contains m "Bcc: baz"))
    (is (not (.contains m "To: ")))))
