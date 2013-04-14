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

(ns postal.stress
  (:import [java.util Date]
           [java.text SimpleDateFormat]
           [java.util.concurrent CountDownLatch])
  (:use [postal.smtp :only [smtp-send]]
        [postal.message :only [make-fixture]]))

(def ^SimpleDateFormat DATEFORMAT (SimpleDateFormat. "yyyy-MM-dd.HH:mm:ss"))

(defonce counter (atom 0))

(defn reset-counter! []
  (reset! counter 0))

(def logger (agent nil))

(defn log* [x s]
  (let [s* (format "%s %s"
                   (.format DATEFORMAT (Date.))
                   (apply str (interpose " " s)))]
    (println s*)
    (flush)
    s*))

(defn log [& s]
  (send-off logger log* s))

(defn partition-work
  "Break up total number of messages evenly over desired number of threads."
  [n ts]
  (if (> ts n)
    [n]
    (let [m (int (Math/floor (float (/ n ts))))]
      (loop [acc [] n n]
        (if (>= n m)
          (recur (conj acc m) (- n m))
          (if (pos? n)
            (conj acc n)
            acc))))))

(defn spam
  ([host port from to n]
     (spam host port from to n 0))
  ([host port from to n delay]
     (log (format "(thread: %s) %s msgs -> %s"
                  (-> (Thread/currentThread) .getId)
                  n host))
     (let [date (.format DATEFORMAT (Date.))]
       (dotimes [x n]
         (smtp-send host port (make-fixture from to))
         (swap! counter inc)
         (Thread/sleep delay))
       n))
  ([host port from to n delay threads]
     (let [latch (CountDownLatch. threads)
           res (doall
                (map #(future
                        (let [ct (spam host port from to % delay)]
                          (.countDown latch)
                          ct))
                     (partition-work n threads)))]
       (.await latch)
       (reduce #(+ %1 (deref %2)) 0 res))))
