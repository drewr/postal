(defproject com.draines/postal "1.12-SNAPSHOT"
  :description "JavaMail on Clojure"
  :url "https://github.com/drewr/postal"
  :license {:name "MIT"
            :url "http://en.wikipedia.org/wiki/MIT_License"
            :distribution :repo
            :comments "Use at your own risk"}
  :repositories {"java.net" "http://download.java.net/maven/2"}
  :dependencies [[commons-codec "1.9"]
                 [com.sun.mail/javax.mail "1.5.5"
                  :exclusions [javax.activation/activation]]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]]}})
