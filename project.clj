(defproject com.intception/postal "3.0.1"
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
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]]}})
