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

(ns postal.support
  (:require [clojure.java.io :as io])
  (:import (java.util Properties Random)
           (org.apache.commons.codec.binary Base64)))

(def boolean-props
  #{:allow8bitmime
    :auth :auth.login.disable :auth.plain.disable :auth.digest-md5.disable :auth.ntlm.disable
    :ehlo
    :noop.strict
    :quitwait
    :reportsuccess
    :sasl.enable :sasl.usecanonicalhostname
    :sendpartial
    :socketFactory.fallback
    :ssl.enable :ssl.checkserveridentity
    :starttls.enable :starttls.required
    :userset})

(defmacro do-when
  [arg condition & body]
  `(when ~condition
     (doto ~arg ~@body)))

(defn prop-names [sender {:keys [user ssl] :as params}]
  (let [prop-map {:tls :starttls.enable}
        defaults {:host "not.provided"
                  :port (if ssl 465 25)
                  :auth (boolean user)}]
    (into {} (apply concat
                    (for [[k v] (merge defaults
                                       (if sender {:from sender} {})
                                       (dissoc params :pass :ssl :proto))
                          :when (not (nil? v))
                          :let [k (if (prop-map k) (prop-map k) k)
                                v (if (or (instance? Boolean v) (boolean-props k))
                                    (if v "true" "false")
                                    v)]]
                      (if (keyword? k)
                        [[(str "mail.smtp." (name k)) v]
                         [(str "mail.smtps." (name k)) v]]
                        [[k v]]))))))

(defn make-props [sender params]
  (let [p (Properties.)]
    (doseq [[k v] (prop-names sender params)]
      (.put p k v))
    p))

(defn hostname []
  (.getHostName (java.net.InetAddress/getLocalHost)))

(defn message-id
  ([]
     (message-id (format "postal.%s" (hostname))))
  ([host]
     (let [bs (byte-array 16)
           r (Random.)
           _ (.nextBytes r bs)
           rs (String. (Base64/encodeBase64 bs))
           onlychars (apply str (re-seq #"[0-9A-Za-z]" rs))
           epoch (.getTime (java.util.Date.))]
       (format "<%s.%s@%s>" onlychars epoch host))))

(defn pom-version []
  (when-let [is (some-> (io/resource "META-INF/maven/com.draines/postal/pom.properties")
                        io/input-stream)]
    (.getProperty (doto (Properties.)
                    (.load is))
                  "version")))

(defn user-agent []
  (if-let [version (or (System/getProperty "postal.version")
                       (pom-version))]
    (format "postal/%s"
            version)
    "postal"))
