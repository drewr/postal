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

(ns postal.core
  (:use [postal.sendmail :only [sendmail-send]]
        [postal.smtp :only [smtp-send]]
        [postal.stress :only [spam]]))

(defn send-message
  ([{:keys [host] :as server}
    {:keys [from to subject body] :or {to "" subject ""} :as msg}]
     (when-not (and from to)
       (throw (Exception. "message needs at least :from and :to")))
     (if host
       (smtp-send server msg)
       (sendmail-send msg)))
  ([msg]
     (send-message (meta msg) msg)))

(defn stress [profile]
  (let [defaults #^{:host "localhost"
                    :port 25
                    :num 1
                    :delay 100
                    :threads 1}
        {:from "foo@lolz.dom"
         :to "bar@lolz.dom"}
        {:keys [host port from to num delay threads]}
        (merge (meta defaults) defaults (meta profile) profile)]
    (println (format "sent %s msgs to %s:%s"
                     (spam host port from to num delay threads)
                     host port))))
