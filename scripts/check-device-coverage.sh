#!/usr/bin/env sh
# Fail when the device tests cover too little of the app module.
#
# SonarQube cannot see coverage from device tests, so the app module is excluded
# from its coverage measure and checked here instead. See
# docs/decisions/static-analysis-and-coverage-in-ci.md.
#
# Only handwritten code is measured. Room writes a *_Impl.kt file for the
# database and for each DAO; most of their lines are schema checks that no test
# of ours can reach, so they are left out.
#
# Usage: check-device-coverage.sh <JaCoCo XML report> [minimum line coverage, percent]
# The report comes from ./gradlew createDebugAndroidTestCoverageReport.
set -eu

report=${1:-}
minimum=${2:-80}

if [ ! -r "$report" ]; then
    echo >&2 "device coverage: no report at '$report'. Run ./gradlew createDebugAndroidTestCoverageReport with an emulator connected."
    exit 1
fi

# The report is one long line, so split it into one tag per line first. Each
# source file ends with its own line totals; counters outside a source file
# belong to classes, methods, packages, and the whole report, and would count
# the same lines again.
totals=$(tr '>' '\n' < "$report" | awk '
    function number(tag, key,    rest) {
        rest = tag
        sub(".* " key "=\"", "", rest)
        sub("\".*", "", rest)
        return rest + 0
    }
    /<sourcefile / {
        in_file = ($0 !~ /\/$/)
        generated = ($0 ~ /_Impl\.kt"/)
        lines = ""
    }
    /<counter type="LINE"/ { if (in_file) lines = $0 }
    /<\/sourcefile/ {
        if (in_file && !generated && lines != "") {
            missed += number(lines, "missed")
            covered += number(lines, "covered")
        }
        in_file = 0
    }
    END { print missed + 0, covered + 0 }
')
missed=${totals% *}
covered=${totals#* }
if [ $((missed + covered)) -eq 0 ]; then
    echo >&2 "device coverage: '$report' has no line totals, so coverage cannot be checked."
    exit 1
fi

# Whole percent, rounded down, so 79.9 does not pass a minimum of 80.
percent=$((covered * 100 / (missed + covered)))
if [ "$percent" -lt "$minimum" ]; then
    echo >&2 "device coverage: device tests cover $percent% of the app module's handwritten lines ($covered of $((missed + covered))); the minimum is $minimum%."
    echo >&2 "Open app/build/reports/coverage/androidTest/debug/connected/index.html to see what is untested."
    exit 1
fi
echo "device coverage: $percent% of the app module's handwritten lines ($covered of $((missed + covered)))."
