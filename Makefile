HERE = $(shell pwd)

default:
	@env SRC=src \
	    MAIN=com.draines.postal.main \
	    PROJECT=postal \
	    DEST=${HERE}/dist \
	    bin/make-jar
