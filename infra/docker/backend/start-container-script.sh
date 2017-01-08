#!/bin/sh
set -e
set -x
# This script will be executed once on a `docker run`.

# start Apache ACE client
java \
      -Dgosh.args=--nointeractive \
      -Dorg.apache.felix.http.jetty.maxFormSize=1000000000 \
      -Dfelix.fileinstall.dir=load \
      -Dorg.osgi.service.http.port=OSGI_SERVICE_HTTP_PORT \
      -Dosgi.shell.telnet.ip=OSGI_SHELL_TELNET_IP \
      -Dosgi.shell.telnet.port=OSGI_SHELL_TELNET_PORT \
      -jar quizzical-backend.jar