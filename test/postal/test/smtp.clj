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
             "mail.smtp.host" "smtp.bar.dom"}))
