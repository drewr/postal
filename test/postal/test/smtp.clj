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

(ns postal.test.smtp
  (:use [clojure.test])
  (:require [postal.smtp :as smtp] :reload))

(defn props [attrs]
  (let [msg {:from "foo@bar.dom"
             :to "baz@bar.dom"
             :subject "Test"
             :body "Hello."}]
    (binding [smtp/smtp-send* (fn [& args] args)]
      (->>
       (smtp/smtp-send attrs [msg])
       first
       .getProperties
       (into {})))))

(defmacro is-props [input want]
  `(is (= (props ~input) ~want)))

(deftest t-props
  (is-props {:host "smtp.bar.dom"}
            {"mail.smtp.port" 25
             "mail.smtp.auth" "false"
             "mail.smtp.host" "smtp.bar.dom"})
  (is-props {:host "smtp.bar.dom"
             :user "foo"
             :pass "pass"}
            {"mail.smtp.user" "foo"
             "mail.smtp.port" 25
             "mail.smtp.auth" "true"
             "mail.smtp.host" "smtp.bar.dom"})
  (is-props {:host "smtp.bar.dom"
             :user "foo"
             :pass "pass"
             :ssl :y}
            {"mail.smtp.user" "foo"
             "mail.smtp.port" 465
             "mail.smtp.auth" "true"
             "mail.smtp.host" "smtp.bar.dom"})
  (is-props {:host "smtp.bar.dom"
             :user nil
             :pass nil}
            {"mail.smtp.port" 25
             "mail.smtp.auth" "false"
             "mail.smtp.host" "smtp.bar.dom"}))
