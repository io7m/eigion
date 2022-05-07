#!/bin/sh

exec /usr/bin/env java \
-p launcher/modules \
-m com.io7m.eigion.launcher.main/com.io7m.eigion.launcher.main.EILauncherMain \
"$@"
