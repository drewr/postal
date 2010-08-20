(defproject com.draines/postal "1.3.2-SNAPSHOT"
  :resources-path "etc"
  :repositories {"java.net" "http://download.java.net/maven/2"}
  :dependencies [[org.clojure/clojure "1.2.0-master-SNAPSHOT"]
                 [org.clojure/clojure-contrib "1.2.0-SNAPSHOT"]
                 [javax.mail/mail "1.4.4-SNAPSHOT"
                  :exclusions [javax.activation/activation]]]
  :dev-dependencies [[swank-clojure "1.2.1"]]
  :source-path "src/clj"
  :main com.draines.postal.main)
