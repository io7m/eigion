#!/bin/sh

exec /usr/bin/env java \
-p workbench/modules \
-m com.io7m.eigion.distribution.example/com.io7m.eigion.distribution.example.EIExampleMain \
"$@"
