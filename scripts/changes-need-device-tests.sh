#!/usr/bin/env sh
# Decide whether a set of changed files must be verified on a device.
#
# Storage, screens, and notifications all live in the app module and can only
# be tested on an emulator or phone, so any change under app/ needs the device
# tests. The pure Kotlin module is covered by the JVM tests instead.
#
# Usage: <list of changed paths, one per line> | changes-need-device-tests.sh
# Exit 0 when the device tests are needed, 1 when they are not.
set -eu

if grep -q '^app/'; then
    exit 0
fi
exit 1
