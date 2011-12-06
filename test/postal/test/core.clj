(ns postal.test.core
  (:use [postal.core]
        [postal.sendmail :only [sendmail-send]]
        [postal.smtp :only [smtp-send]]
        [postal.stress :only [spam]]))
