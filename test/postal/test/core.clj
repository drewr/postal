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
