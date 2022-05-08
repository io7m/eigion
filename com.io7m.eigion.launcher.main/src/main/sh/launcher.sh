#!/bin/sh

exec /usr/bin/env java \
-p launcher/boot \
-m com.io7m.eigion.launcher.main/com.io7m.eigion.launcher.main.EILauncherMain \
"$@"
