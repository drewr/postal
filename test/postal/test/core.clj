(ns postal.test.core
  (:require [postal.core :as postal]
            [postal.sendmail :as local]
            [postal.smtp :as smtp])
  (:use [clojure.test]))

(defn sendmail-send [msg]
  (merge {:which :sendmail} msg (meta msg)))

(defn smtp-send [server msg]
  (merge {:which :smtp} server msg (meta msg)))

(deftest integration
  (with-redefs [local/sendmail-send sendmail-send
                smtp/smtp-send smtp-send]
    (is (= :smtp (:which
                  (postal/send-message ^{:host "localhost"
                                         :port "25"}
                                       {:from "foo@example.com"
                                        :to "bar@example.com"}))))
    (is (= :sendmail (:which
                      (postal/send-message {:from "foo@example.com"
                                            :to "bar@example.com"}))))))
