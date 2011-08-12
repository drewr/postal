(defproject com.draines/postal "1.6-SNAPSHOT"
  :resources-path "etc"
  :repositories {"java.net" "http://download.java.net/maven/2"
                 "clojars" "http://clojars.org/repo"}
  :dependencies [[org.clojure/clojure "1.3.0-beta1"]
                 [javax.mail/mail "1.4.4"
                  :exclusions [javax.activation/activation]]]
  :dev-dependencies [[swank-clojure "1.3.1"]
                     [lein-clojars "0.6.0"]])
