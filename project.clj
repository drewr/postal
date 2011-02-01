(defproject com.draines/postal "1.4.0-SNAPSHOT"
  :resources-path "etc"
  :repositories {"java.net" "http://download.java.net/maven/2"}
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [javax.mail/mail "1.4.4-SNAPSHOT"
                  :exclusions [javax.activation/activation]]]
  :dev-dependencies [[swank-clojure "1.2.1"]]
  :source-path "src/clj"
  :main com.draines.postal.main)
