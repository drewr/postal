(ns postal.test.stress
  (:import [java.util Date]
           [java.text SimpleDateFormat]
           [java.util.concurrent CountDownLatch])
  (:use [clojure.test]
        [postal.stress]
        [postal.smtp :only [smtp-send]]
        [postal.message :only [make-fixture]]))


(deftest test-partition-work
  (is (= [5 5] (partition-work 10 2)))
  (is (= [3 3 3 1] (partition-work 10 3)))
  (is (= [111 111 111 111 111 111 111 111 111 111 3]
         (partition-work 1113 10))))
