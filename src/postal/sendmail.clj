;; Copyright (c) Andrew A. Raines
;;
;; Permission is hereby granted, free of charge, to any person
;; obtaining a copy of this software and associated documentation
;; files (the "Software"), to deal in the Software without
;; restriction, including without limitation the rights to use,
;; copy, modify, merge, publish, distribute, sublicense, and/or sell
;; copies of the Software, and to permit persons to whom the
;; Software is furnished to do so, subject to the following
;; conditions:
;;
;; The above copyright notice and this permission notice shall be
;; included in all copies or substantial portions of the Software.
;;
;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
;; EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
;; OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
;; NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
;; HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
;; WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
;; FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
;; OTHER DEALINGS IN THE SOFTWARE.

(ns postal.sendmail
  (:use [postal.message :only [message->str sender recipients]]))

(def sendmails ["/usr/lib/sendmail"
                "/usr/sbin/sendmail"
                "/usr/bin/sendmail"
                "/usr/local/lib/sendmail"
                "/usr/local/sbin/sendmail"
                "/usr/local/bin/sendmail"
                "/usr/sbin/msmtp"])

(def errors {0  [:SUCCESS        "message sent"]
             64 [:EX_USAGE       "command line usage error"]
             65 [:EX_DATAERR     "data format error"]
             66 [:EX_NOINPUT     "cannot open input"]
             67 [:EX_NOUSER      "addressee unknown"]
             68 [:EX_NOHOST      "host name unknown"]
             69 [:EX_UNAVAILABLE "service unavailable"]
             70 [:EX_SOFTWARE    "internal software error"]
             71 [:EX_OSERR       "system error (no fork?)"]
             72 [:EX_OSFILE      "critical OS file missing"]
             73 [:EX_CANTCREAT   "can't create (user) output file"]
             74 [:EX_IOERR       "input/output error"]
             75 [:EX_TEMPFAIL    "temp failure; user is invited to retry"]
             76 [:EX_PROTOCOL    "remote error in protocol"]
             77 [:EX_NOPERM      "permission denied"]
             78 [:EX_CONFIG      "configuration error"]})

(defn error [code]
  (let [[e message] (errors code)]
    {:code code
     :error e
     :message message}))

(defn sendmail-find []
  (first (filter #(.isFile (java.io.File. %)) sendmails)))

(defn sanitize [text]
  (.replaceAll text "\r\n" (System/getProperty "line.separator")))

(defn sendmail-send [msg]
  (let [mail (sanitize (message->str msg))
        cmd (concat
             [(sendmail-find) (format "-f %s" (sender msg))]
             (recipients msg))
        pb (ProcessBuilder. cmd)
        p (.start pb)
        smtp (java.io.PrintStream. (.getOutputStream p))]
    (.print smtp mail)
    (.close smtp)
    (.waitFor p)
    (error (.exitValue p))))
