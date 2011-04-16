(defproject com.draines/postal "1.5.0-SNAPSHOT"
  :resources-path "etc"
  :repositories {"java.net" "http://download.java.net/maven/2"
                 "clojars" "http://clojars.org/repo"}
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [javax.mail/mail "1.4.4"
                  :exclusions [javax.activation/activation]]]
  :dev-dependencies [[swank-clojure "1.2.1"]
                     [lein-clojars "0.6.0"]])
