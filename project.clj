(defproject com.draines/postal "1.10.0"
  :repositories {"java.net" "http://download.java.net/maven/2"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [commons-codec "1.7"]
                 [javax.mail/mail "1.4.4"
                  :exclusions [javax.activation/activation]]])
