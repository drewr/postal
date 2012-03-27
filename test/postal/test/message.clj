(ns postal.test.message
  (:use [postal.message]
        [clojure.test :only [run-tests deftest is]]
        [postal.date :only [make-date]])
  (:import [java.util Properties UUID]
           [javax.mail Session Message$RecipientType]
           [javax.mail.internet MimeMessage InternetAddress
            AddressException]))

(deftest test-simple
  (let [m {:from "fee@bar.dom"
           :to "Foo Bar <foo@bar.dom>"
           :cc ["baz@bar.dom" "Quux <quux@bar.dom>"]
           :date (java.util.Date.)
           :subject "Test"
           :body "Test!"}]
    (is (= "Subject: Test" (re-find #"Subject: Test" (message->str m))))
    (is (re-find #"Cc: baz@bar.dom, Quux <quux@bar.dom>" (message->str m)))))

(deftest test-multipart
  (let [m {:from "foo@bar.dom"
           :to "baz@bar.dom"
           :subject "Test"
           :body [{:type "text/html"
                   :content "<b>some html</b>"}]}]
    (is (= "multipart/mixed" (re-find #"multipart/mixed" (message->str m))))
    (is (= "Content-Type: text/html"
           (re-find #"Content-Type: text/html" (message->str m))))
    (is (= "some html" (re-find #"some html" (message->str m))))))

(deftest test-inline
  (let [f (doto (java.io.File/createTempFile "_postal-" ".txt"))
        _ (doto (java.io.PrintWriter. f)
            (.println "tempfile contents") (.close))
        m {:from "foo@bar.dom"
           :to "baz@bar.dom"
           :subject "Test"
           :body [{:type :inline
                   :content f}]}]
    (is (= "tempfile" (re-find #"tempfile" (message->str m))))
    (.delete f)))

(deftest test-attachment
  (let [f1 (doto (java.io.File/createTempFile "_postal-" ".txt"))
        _ (doto (java.io.PrintWriter. f1)
            (.println "tempfile contents") (.close))
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

(deftest test-nested
  (let [f (doto (java.io.File/createTempFile "_postal-" ".txt"))
        _ (doto (java.io.PrintWriter. f)
            (.println "tempfile contents") (.close))
        m {:from "foo@bar.dom"
           :to "baz@bar.dom"
           :subject "Test"
           :body [[:alternative
                   {:type "text/html"
                    :content "<b>some html</b>"}
                   {:type "text/plain"
                    :content "some text"}]
                  {:type :attachment
                   :content f}]}]
    (is (= "multipart/mixed" (re-find #"multipart/mixed" (message->str m))))
    (is (= "multipart/alternative"
           (re-find #"multipart/alternative" (message->str m))))
    (is (= "Content-Type: text/html"
           (re-find #"Content-Type: text/html" (message->str m))))
    (is (= "some html" (re-find #"some html" (message->str m))))
    (is (= "Content-Type: text/plain"
           (re-find #"Content-Type: text/plain" (message->str m))))
    (is (= "some text" (re-find #"some text" (message->str m))))
    (is (= "tempfile" (re-find #"tempfile" (message->str m))))
    (.delete f)))

(deftest test-fixture
  (let [from "foo@bar.dom"
        to "baz@bar.dom"
        tag "[TEST]"]
    (is (re-find #"^\[TEST" (:subject (make-fixture from to :tag tag))))))

(deftest test-extra-headers
  (let [m {:from "foo@bar.dom"
           :to "baz@bar.dom"
           :subject "Test"
           :User-Agent "Lorem Ipsum"
           :body "Foo!"}]
    (is (re-find #"User-Agent: Lorem Ipsum" (message->str m)))))

(deftest test-bad-addrs
  (let [m {:from "foo @bar.dom"
           :to "badddz@@@bar.dom"
           :subject "Test"
           :body "Bad recipient!"}]
    (is (not (re-find #"badddz" (message->str m))))
    (is (not (re-find #"foo @bar" (message->str m))))))

(deftest test-reply-to
  (let [m {:from "foo@bar.dom"
           :to "baz@bar.dom"
           :subject "Test"
           :body "Reply me!"
           :reply-to "yermom@bar.dom"}]
    (is (re-find #"Reply-To: yermom" (message->str m)))))
