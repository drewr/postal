Postal
=======

#### Internet email library for Clojure

Postal is a library for constructing and sending RFC822-compliant
Internet email messages.  It wraps the JavaMail package for message
and SMTP support.  It supports sendmail natively.

### Platforms

* Any Unix platform that supports Java and sendmail (`msmtp`, et al.)
* Untested on Windows

### Examples

At a bare minimum, provide a map with `:from`, `:to`, `:subject`, and `:body`.
This will locally inject the message into sendmail.

    user> (in-ns 'com.draines.postal.core)
    #<Namespace com.draines.postal.core>
    com.draines.postal.core> (send-message {:from "me@draines.com"
                                            :to ["mom@example.com" "dad@example.com"]
                                            :cc "bob@example.com"
                                            :subject "Hi!"
                                            :body "Test."})
    {:code 0, :error :SUCCESS, :message "message sent"}
    com.draines.postal.core> 

To use SMTP, add metadata with a `:host` key.

    com.draines.postal.core> (send-message #^{:host "mail.isp.net"}
                                           {:from "me@draines.com"
                                            :to "foo@example.com"
                                            :subject "Hi!"
                                            :body "Test."})
    {:code 0, :error :SUCCESS, :message "message sent"}
    com.draines.postal.core> 

Attachments and multipart messages can be added as sequences of maps:

    com.draines.postal.core> (send-message #^{:host "mail.isp.net"}
                                           {:from "me@draines.com"
                                            :to "foo@example.com"
                                            :subject "Hi!"
                                            :body [{:type "text/html"
                                                    :content "<b>Test!</b>"}
                                                ;;;; supports both dispositions:
                                                   {:type :attachment
                                                    :content (java.io.File. "/tmp/foo.txt")}
                                                   {:type :inline
                                                    :content (java.io.File. "/tmp/foo.txt")}]})
    {:code 0, :error :SUCCESS, :message "message sent"}
    com.draines.postal.core>


### Building

    % ant dist
    % ls dist
    postal-20090726194310+32b4b1f.jar


## License

Postal is (c) 2009 Drew Raines and released under the MIT license.
