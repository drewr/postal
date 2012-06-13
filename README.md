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

* JavaMail 1.4.2 (in `lib/` after build)

### Install

Served by Clojars.  In your project.clj:

    [com.draines/postal "1.8.0"]

Likewise substitute any tag, like `1.6.0` etc.

### Examples

#### Local

At a bare minimum, provide a map with `:from` and `:to` (and you'll
probably also be wanting `:subject` and `:body`, though they're
technically optional).  Any other keys you supply will show up as
ancillary headers.  This example will locally inject the message into
sendmail.

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

To use SMTP, add metadata with a `:host` key.

    postal.core> (send-message ^{:host "mail.isp.net"}
                               {:from "me@draines.com"
                                :to "foo@example.com"
                                :subject "Hi!"
                                :body "Test."})
    {:code 0, :error :SUCCESS, :message "message sent"}
    postal.core>

#### Authentication

Authenticate to SMTP server with `:user` and `:pass`.

    postal.core> (send-message ^{:host "mail.isp.net"
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

    postal.core> (send-message ^{:host "smtp.gmail.com"
                                 :user "jsmith"
                                 :pass "sekrat!!1"
                                 :ssl :yes!!!11}
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

    postal.core> (send-message ^{:user "AKIAIDTP........" :pass "AikCFhx1P......."
                                 :host "email-smtp.us-east-1.amazonaws.com"
                                 :port 587}
                   {:from "me@draines.com" :to "me@draines.com"
                    :subject "Test from Amazon SES" :body "Test!!!11"})
    {:error :SUCCESS, :code 0, :message "messages sent"}
    postal.core>

#### Attachments

Attachments and multipart messages can be added as sequences of maps:

    postal.core> (send-message ^{:host "mail.isp.net"}
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

If your attachment has a content-type that is not recognized by JavaMail, e.g.,
`.pdf` or `.doc`, you can set `:content-type`.

If you want another multipart type than "mixed", you can specify it as a keyword
as the first value in the map sequence. That way you can for example create an
HTML-Email that displays a text message as fallback in email clients that do not
support (or suppress) HTML-mails:

    postal.core> (send-message ^{:host "localhost"
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

Postal uses JavaMail under the covers, which defaults to charset
`us-ascii`. To set the charset, set the `:type`, like `"text/html; charset=utf-8"`

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

André Branco
Joe Gallo
Christoph Henkelmann
Gerrit Hentschel
Jeff Palmucci
Paul Stadig
Sam Ritchie

## License

Postal is (c) 2009-2011 Andrew A. Raines and released under the MIT license.
