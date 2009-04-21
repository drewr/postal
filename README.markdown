Postal
=======

#### Internet email library for Clojure

Postal is a library for constructing and sending RFC822-compliant
Internet email messages.  It wraps the JavaMail package for message
and SMTP support.  It supports sendmail natively.

### Platforms

* Any Unix platform that supports Java and sendmail (`msmtp`, et al.)
* Untested on Windows

### Dependencies

* JavaMail ([download](http://draines.com/dist/java/mail-1.4.2.jar))

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

To use SMTP, add a `:host` key.

    com.draines.postal.core> (send-message {:host "mail.isp.net"
                                            :from "me@draines.com"
                                            :to "foo@example.com"
                                            :subject "Hi!"
                                            :body "Test."})
    {:code 0, :error :SUCCESS, :message "message sent"}
    com.draines.postal.core> 



### Building

    % env JARHOME=$HOME/tmp/src/jar make
    Packaging /Users/aar/tmp/src/jar/clojure.jar
    Packaging /Users/aar/tmp/src/jar/clojure-contrib.jar
    Packaging /Users/aar/tmp/src/jar/mail.jar
    Compiling com.draines.postal.main to classes
    Compiling com.draines.postal.core to classes
    Compiling com.draines.postal.date to classes
    Compiling com.draines.postal.message to classes
    Compiling com.draines.postal.sendmail to classes
    Compiling com.draines.postal.smtp to classes
    Writing /Users/aar/src/postal/dist/postal-20090420184320-980d1c20.jar

## License

Postal is (c) 2009 by Drew Raines and released under the MIT license.
