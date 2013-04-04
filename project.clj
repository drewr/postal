(defproject com.draines/postal "1.10.1"
  :repositories {"java.net" "http://download.java.net/maven/2"}
  :dependencies [[commons-codec "1.7"]
                 [javax.mail/mail "1.4.4"
                  :exclusions [javax.activation/activation]]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}})
