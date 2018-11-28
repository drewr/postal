(defproject com.draines/postal "2.0.3-SNAPSHOT"
  :description "JavaMail on Clojure"
  :url "https://github.com/drewr/postal"
  :license {:name "MIT"
            :url "http://en.wikipedia.org/wiki/MIT_License"
            :distribution :repo
            :comments "Use at your own risk"}
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[commons-codec "1.9"]
                 [com.sun.mail/javax.mail "1.5.5"]
                 [javax.mail/javax.mail-api "1.5.5"]]
  :global-vars {*warn-on-reflection* true}
  :profiles {:dev {:dependencies []}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}}
  :aliases {"test-all" ["with-profile" "dev,1.7:dev,1.8:dev,1.9" "test"]})
