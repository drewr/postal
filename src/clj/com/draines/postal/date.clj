(ns com.draines.postal.date
  (:import [java.util Date]
           [java.text SimpleDateFormat ParsePosition]))

(defn make-date
  ([tmpl s]
     (.parse (SimpleDateFormat. tmpl) s (ParsePosition. 0)))
  ([]
     (Date.)))