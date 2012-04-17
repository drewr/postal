(defproject com.draines/postal "1.8-SNAPSHOT"
  :repositories {"java.net" "http://download.java.net/maven/2"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [javax.mail/mail "1.4.4"
                  :exclusions [javax.activation/activation]]
                 [org.clojure.contrib/repl-utils "1.3.0-alpha4"]])
