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

(ns postal.test.message
  (:use [postal.message]
        [clojure.test :only [run-tests deftest is]]
        [clojure.java.io :as io]
        [postal.date :only [make-date]])
  (:import [java.util Properties UUID]
           [javax.mail Session Message$RecipientType]
           [javax.mail.internet MimeMessage InternetAddress
            AddressException]
           [java.util.zip ZipOutputStream ZipEntry]))

(deftest test-simple
  (let [m (message->str
           {:from "fee@bar.dom"
            :to "Foo Bar <foo@bar.dom>"
            :cc ["baz@bar.dom" "Quux <quux@bar.dom>"]
            :date (java.util.Date.)
            :subject "Test"
            :body "Test!"
            :charset "us-ascii"})]
    (is (= "Subject: Test" (re-find #"Subject: Test" m)))
    (is (re-find #"Cc: baz@bar.dom, Quux <quux@bar.dom>" m))
    (is (re-find #"(?i)content-type:.*us-ascii" m))))

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

(deftest test-multipart-with-custom-name-and-description
  (let [m (message->str
           {:from "foo@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body [{:type "text/plain"
                    :content "See attached"}
                   {:type "text/csv"
                    :file-name "data.csv"
                    :description "Some interesting data"
                    :content "x,2x\n1,2,\n2,4\n"}]})]
    (is (.contains m "See attached"))
    (is (.contains m "data.csv"))
    (is (.contains m "Some interesting data"))
    (is (.contains m "x,2x\n1,2,\n2,4\n"))))

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
    (is (.contains m (.getName f1)))
    (is (.contains m "resolv.conf"))
    (is (not (.contains m "etc")))
    (.delete f1)))

(deftest test-attachment-from-bytes
  (let [b (.getBytes "foo")
        m (message->str
            {:from "x@x.dom"
             :to "y@y.dom"
             :subject "Test"
             :body [{:type :attachment
                     :content b
                     :content-type "bar/bar"
                     :file-name "baz"}]})]
    (is (.contains m "foo"))
    (is (.contains m "bar/bar"))
    (is (.contains m "baz"))))

(deftest test-attachments-from-url
  (let [jar (doto (java.io.File/createTempFile "_postal-" ".jar"))
        _ (with-open [zip-out (ZipOutputStream. (io/output-stream jar))
                      zip-w (writer zip-out)]
            (.putNextEntry zip-out (ZipEntry. "test-directory/test-filename.txt"))
            (binding [*out* zip-w]
              (println "tempfile contents")))
        jar-url (str "jar:file:" (.getPath jar) "!/test-directory/test-filename.txt")
        f-url "file:///etc/resolv.conf"
        m (message->str
            {:from "foo@bar.dom"
             :to "baz@bar.dom"
             :subject "Test"
             :body [{:type :attachment
                     :content jar-url}
                    {:type :attachment
                     :content f-url}]})]
    (is (.contains m "tempfile"))
    (is (.contains m "test-filename.txt"))
    (is (not (.contains m "test-directory")))
    (is (.contains m "resolv.conf"))
    (is (not (.contains m "etc")))
    (.delete jar)))

(deftest test-attachment-with-custom-name-and-description
  (let [f1 (doto (java.io.File/createTempFile "_postal-" ".txt"))
        _ (doto (java.io.PrintWriter. f1)
            (.println "tempfile contents") (.close))
        m (message->str
           {:from "foo@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body [{:type "text/plain"
                    :content "See attached"}
                   {:type :attachment
                    :file-name "ImportantDocumentA.txt"
                    :description "A document that we should all marvel at"
                    :content f1}]})]
    (is (.contains m "tempfile"))
    (is (.contains m "ImportantDocumentA.txt"))
    (is (.contains m "A document that we should all marvel at"))
    (.delete f1)))

(deftest test-attachment-with-unicode-name
  (let [f1 (doto (java.io.File/createTempFile "_postal-" ".txt"))
        _ (doto (java.io.PrintWriter. f1)
            (.println "tempfile contents") (.close))
        m (message->str
           {:from "foo@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body [{:type "text/plain"
                    :content "See attached"}
                   {:type :attachment
                    :file-name "Важный документ.txt"
                    :content f1}]})]
    (is (.contains m "tempfile"))
    (is (.contains m "=?UTF-8?B?0JLQsNC20L3Ri9C5INC00L7QutGD0LzQtdC90YIudHh0?="))
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
    (is (zero? (.indexOf ^String (:subject (make-fixture from to :tag tag)) "[TEST")))))

(deftest test-extra-headers
  (let [m {:from "foo@bar.dom"
           :to "baz@bar.dom"
           :subject "Test"
           :User-Agent "Lorem Ipsum"
           :Content-Type "text/html"
           :body "<html><body>Foo!</body></html>"}]
    (is (.contains (message->str m) "User-Agent: Lorem Ipsum"))
    (is (= (.getContentType (make-jmessage m)) "text/html"))))

(deftest test-bad-addrs
  (let [m (message->str
           {:from "foo @bar.dom"
            :to "badddz@@@bar.dom"
            :subject "Test"
            :body "Bad recipient!"})]
    (is (not (.contains m "badddz")))
    (is (not (.contains m "foo @bar")))))

(deftest test-reply-to
  (let [m {:from "foo@bar.dom"
           :to "baz@bar.dom"
           :subject "Test"
           :body "Reply me!"
           :reply-to "yermom@bar.dom"}]
    (is (re-find #"Reply-To: yermom" (message->str m)))))

(deftest test-charsets
  (let [m (message->str
           {:from "From íč <p@p.com>"
            :to "To Böb <bob@bar.dom>"
            :cc ["Plain Addr <plain@bar.dom>"]
            :subject "Subject æøå" ;; Norwegian
            :body "Charsets!"})]
    (is (.contains m "From_=C3=AD=C4=8D?="))
    (is (.contains m "To_B=C3=B6b?="))
    (is (.contains m "Plain Addr"))
    (is (.contains m "Subject_=C3=A6=C3=B8=C3=A5?=")))
  (let [m (message->str
           {:from "íč <p@p.com>"
            :to "Böb <bob@bar.dom>"
            :cc ["Plain Addr <plain@bar.dom>"]
            :subject "Test"
            :charset "iso-8859-1"
            :body "Charsets!"})]
    (is (.contains m "Content-Type: text/plain; charset=iso-8859-1"))
    (is (.contains m "=?iso-8859-1?B?7T8=?="))
    (is (.contains m "Plain Addr")))
  (let [m (message->str
           {:from "foo@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body "Reply me!"
            :reply-to "yermom@bar.dom"})]
    (is (.contains m "Reply-To: yermom"))))

(deftest test-make-address-to
  (let [m (message->str
           {:from "foo@bar.dom"
            :to (make-address "bob@bar.dom" "To > Bob" "UTF-8")
            :subject "Test"
            :body "Test"})]
    (is (.contains m "\"To > Bob\" <bob@bar.dom>"))))

(deftest test-only-bcc
  (let [m (message->str
           {:from "foo@bar.dom"
            :bcc "baz@bar.dom"
            :subject "Test"
            :body "Only Bcc!!"})]
    (is (.contains m "Bcc: baz"))
    (is (not (.contains m "To: ")))))

(deftest test-message-id
  (let [m (message->str
           {:from "foo@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body "Where is that message ID!"})]
    (is (re-find #"Message-ID: <.*?@postal\..*>" m)))
  (let [m (message->str
           {:from "foo@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body "Where is that message ID!"
            :message-id #(postal.support/message-id "foo.bar.dom")})]
    (is (.contains m "@foo.bar.dom")))
  (let [m (message->str
           {:from "foo@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body "Where is that message ID!"
            :message-id (fn [] "foo")})]
    (is (.contains m "Message-ID: foo"))))

(deftest test-user-agent
  (let [m (message->str
           {:from "foo@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body "Where is that message ID!"
            :user-agent "foo/1.0"})]
    (is (.contains m "User-Agent: foo"))))

(deftest test-sender
  (let [m (message->str
           {:sender "sender@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body "Test!"})]
    (is (.contains m "From: sender@bar.dom"))))

(deftest test-from-and-sender
  (let [m (message->str
           {:sender "sender@bar.dom"
            :from "from@bar.dom"
            :to "baz@bar.dom"
            :subject "Test"
            :body "Test!"})]
    (is (.contains m "From: from@bar.dom"))))
