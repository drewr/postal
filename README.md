[![CircleCI](https://circleci.com/gh/drewr/postal/tree/master.svg?style=svg)](https://circleci.com/gh/drewr/postal/tree/master)

postal
=======

#### Internet email library for Clojure

postal is a library for constructing and sending RFC822-compliant
Internet email messages.  It wraps the JavaMail package for message
and SMTP support.  It supports sendmail natively.  Supports STARTTLS &
SSL.

### Platforms

* Anything that can run Java should work
* sendmail support likely requires *nix, but `msmtp`, et al., are worthy substitutes
* Untested on Windows

### Dependencies

* JavaMail 1.5.5 (in `lib/` after build)

### Install

Served by Clojars.  In your Leiningen project.clj:

[![Clojars Project](https://img.shields.io/clojars/v/com.draines/postal.svg)](https://clojars.org/com.draines/postal)

Likewise substitute any tag name from git.

### Examples

#### Local

At a bare minimum, provide a map with `:from` and `:to` (and you'll
probably also be wanting `:subject` and `:body`, though they're
technically optional).  Any other keys you supply will show up as
ancillary headers.  This example will locally inject the message into
whatever sendmail-compatible interface your system provides.

    user> (in-ns 'postal.core)
    #<Namespace postal.core>
    postal.core> (send-message {:from "me@draines.com"
                                :to ["mom@example.com" "dad@example.com"]
                                :cc "bob@example.com"
                                :subject "Hi!"
                                :body "Test."
                                :X-Tra "Something else"})
    {:code 0, :error :SUCCESS, :message "message sent"}
    postal.core>

#### SMTP

To use SMTP, add an argument map before the message with at least
`:host` key.

    postal.core> (send-message {:host "mail.isp.net"}
                               {:from "me@draines.com"
                                :to "foo@example.com"
                                :subject "Hi!"
                                :body "Test."})
    {:code 0, :error :SUCCESS, :message "message sent"}
    postal.core>

For legacy compatibility, you can also supply these connection
parameters as metadata on the message.  `(send-message ^{:host ...} {:from ...})`

#### Authentication

Authenticate to SMTP server with `:user` and `:pass`.

    postal.core> (send-message {:host "mail.isp.net"
                                :user "jsmith"
                                :pass "sekrat!!1"}
                               {:from "me@draines.com"
                                :to "foo@example.com"
                                :subject "Hi!"
                                :body "Test."})
    {:code 0, :error :SUCCESS, :message "message sent"}
    postal.core>

#### Encryption (Gmail example)

You probably do not want to do this in the clear, so add `:ssl` to get
an encrypted connection.  This will default to port `465` if you don't
specify one.

If your destination supports TLS instead, you can use `:tls`.  This
will default to port `25`, however, so if you need a different one
make sure you supply `:port`.  (It's common for ISPs to block outgoing
port 25 to relays that aren't theirs.  Gmail supports SSL & TLS but
it's easiest to just use SSL since you'll likely need port 465
anyway.)

    postal.core> (send-message {:host "smtp.gmail.com"
                                :user "jsmith"
                                :pass "sekrat!!1"
                                :ssl true}
                               {:from "me@draines.com"
                                :to "foo@example.com"
                                :subject "Hi!"
                                :body "Test."})
    {:code 0, :error :SUCCESS, :message "message sent"}
    postal.core>

#### Amazon

Since Amazon SES uses authenticated SMTP, postal can use it.  Just
make sure you use a verified address and your SMTP credentials (visit
the AWS Console to set those up).  Also, if you're just sandboxing,
you can only send *to* a verified address as well.  Example:

    postal.core> (send-message {:user "AKIAIDTP........" :pass "AikCFhx1P......."
                                :host "email-smtp.us-east-1.amazonaws.com"
                                :port 587}
                   {:from "me@draines.com" :to "me@draines.com"
                    :subject "Test from Amazon SES" :body "Test!!!11"})
    {:error :SUCCESS, :code 0, :message "messages sent"}
    postal.core>

#### Attachments

Attachments and multipart messages can be added as sequences of maps:

    postal.core> (send-message {:host "mail.isp.net"}
                               {:from "me@draines.com"
                                :to "foo@example.com"
                                :subject "Hi!"
                                :body [{:type "text/html"
                                        :content "<b>Test!</b>"}
                                       ;;;; supports both dispositions:
                                       {:type :attachment
                                        :content (java.io.File. "/tmp/foo.txt")}
                                       {:type :inline
                                        :content (java.io.File. "/tmp/a.pdf")
                                        :content-type "application/pdf"}]})
    {:code 0, :error :SUCCESS, :message "message sent"}
    postal.core>

If your attachment has a content-type that is not recognized by
JavaMail, e.g., `.pdf` or `.doc`, you can set `:content-type`.  You
can also set `:file-name` and `:description` if you don't like the
filename that `:content` uses.

If you want another multipart type than "mixed", you can specify it as a keyword
as the first value in the map sequence. That way you can for example create an
HTML-Email that displays a text message as fallback in email clients that do not
support (or suppress) HTML-mails:

    postal.core> (send-message {:host "localhost"
                                :port 2500
                                :user "user@localhost"
                                :pass "somePassword"}
                           {:from "jon-doe@example.com"
                            :to "jane-doe@example.com"
                            :subject "multipart/alternative test"
                            :body [:alternative
                                   {:type "text/plain"
                                    :content "This is a test."}
                                   {:type "text/html"
                                    :content "<html><head> </head><body>
                                    <h1>Heading 1</h1><p>This is a test.</p>
                                    </body></html>"}
                                  ]}))

#### UTF-8

Postal uses JavaMail underneath, which defaults to charset
`us-ascii`. To set the charset, set the `:type`, like `"text/html; charset=utf-8"`.

#### Message ID

Postal will supply a message ID by default that looks like
`[random]@postal.[host]`.  You can customize this by supplying a
`:message-id` header with a function that takes no args.  The included
`postal.support/message-id` can be used if you'd like to make use of
its randomness and only customize the hostname.

    {:from "foo@bar.dom"
     :to "baz@bar.dom"
     :subject "Message IDs!"
     :body "Regards."
     :message-id #(postal.support/message-id "foo.bar.dom")}

#### User Agent

You can customize the default `User-Agent` header (by default
`postal/VERSION`).

    {:from "foo@bar.dom"
     :to "baz@bar.dom"
     :subject "Message IDs!"
     :body "Regards."
     :user-agent "MyMailer 1.0"}


#### Stress-testing

You can stress-test a server by:

    postal.core> (stress ^{:host "localhost"
                           :num     1000
                           :delay   250   ;; msecs
                           :threads 5     ;; concurrent connections}
                         {:from "foo@lolz.dom"
                          :to "bar@lolz.dom"})
    sent 1000 msgs to localhost:25
    nil
    postal.core>

### Building

    % lein deps && lein jar

## Contributors

Alexander Kouznetsov         
Allen Rohner       
Andre Branco       
Andres Cuervo     
Andy Fingerhut       
Camille Troillard       
Christoph Henkelmann       
Colin Jones       
Dante Briones      
Dimas Guardado       
Gerrit Hentschel       
J. David Lowe       
Jeff Palmucci       
Joe Gallo       
Kevin DeJong       
Kyle Kingsbury      
Paul Biggar       
Paul Stadig       
Phil Hagelberg       
Roman Flammer       
Sam Ritchie       
Stephen Nelson         

## License

Postal is (c) 2009-2019 Andrew A. Raines and released under the MIT
license.
