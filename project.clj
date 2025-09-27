(defproject com.draines/postal "3.0.0-SNAPSHOT"
  :description "Clojure support for Internet email"
  :url "https://github.com/drewr/postal"
  :license {:name "MIT"
            :url "http://en.wikipedia.org/wiki/MIT_License"
            :distribution :repo
            :comments "Use at your own risk"}
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[commons-codec "1.19.0"]
                 [com.sun.mail/jakarta.mail "1.6.7"]]
  :global-vars {*warn-on-reflection* true}
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.12.3"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.1"]]}
             :1.11 {:dependencies [[org.clojure/clojure "1.11.0"]]}
             :1.12 {:dependencies [[org.clojure/clojure "1.12.3"]]}}
  :aliases {"test-all"
            ["with-profile" "1.7:1.8:1.9:1.10:1.11:1.12" "test"]})
