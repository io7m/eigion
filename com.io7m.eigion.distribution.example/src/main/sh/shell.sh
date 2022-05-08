#!/bin/sh

exec /usr/bin/env java \
-p shell/boot \
-m com.io7m.eigion.distribution.example/com.io7m.eigion.distribution.example.EIExampleMain \
"$@"
