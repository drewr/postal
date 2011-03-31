(ns postal.test.smtp
  (:use [postal.message :only [make-jmessage]])
  (:import [javax.mail Transport Session]))