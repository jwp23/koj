#!/usr/bin/env sh
# Fail when a view-model or store method is more complex than the maximum.
#
# Logic written inside a view model or a store cannot be seen by
# scripts/check-app-is-wiring.sh, which looks at file kinds. It leaves a shadow
# in the device-test coverage report, though: the report records the cyclomatic
# complexity of every method, and a method that only passes values between a
# store and a screen scores very little. See docs/decisions/app-module-is-wiring.md.
#
# Only classes from *ViewModel.kt and *Store.kt files are measured, which
# includes the classes the compiler makes for their lambdas. Composables are
# left out because the Compose compiler adds branches of its own to them, and
# Room's generated classes because nobody writes them.
#
# Usage: check-app-complexity.sh <JaCoCo XML report> [maximum complexity | --list]
# The report comes from ./gradlew createDebugAndroidTestCoverageReport.
# With --list, prints every measured method as "<score> <class>.<method>",
# most complex first.
set -eu

report=${1:-}
# A ratchet: the highest score measured when it was last set, so nothing written
# so far fails. Its rationale is in docs/decisions/app-module-is-wiring.md.
maximum=${2:-6}

if [ ! -r "$report" ]; then
    echo >&2 "app complexity: no report at '$report'. Run ./gradlew createDebugAndroidTestCoverageReport with an emulator connected."
    exit 1
fi

# The report is one long line, so split it into one tag per line first. Only a
# COMPLEXITY counter directly inside a method is the method's own; classes and
# packages carry totals after their methods, which would count the same code
# again.
methods=$(tr '>' '\n' < "$report" | awk '
    function attribute(tag, key,    rest) {
        rest = tag
        sub(".* " key "=\"", "", rest)
        sub("\".*", "", rest)
        return rest
    }
    /<class / {
        class = attribute($0, "name")
        gsub("/", ".", class)
        measured = (attribute($0, "sourcefilename") ~ /(ViewModel|Store)\.kt$/)
    }
    /<method / {
        method = attribute($0, "name")
        line = attribute($0, "line")
        in_method = 1
    }
    /<counter type="COMPLEXITY"/ {
        if (in_method && measured) {
            print attribute($0, "missed") + attribute($0, "covered"), class "." method, line
        }
    }
    /<\/method/ { in_method = 0 }
')

if [ "$maximum" = "--list" ]; then
    printf '%s\n' "$methods" | grep . | sort -rn | while read -r score name _line; do
        echo "$score $name"
    done
    exit 0
fi

if [ -z "$methods" ]; then
    echo "app complexity: the report has no view-model or store methods to measure."
    exit 0
fi

offenders=$(printf '%s\n' "$methods" | sort -rn | awk -v maximum="$maximum" '$1 > maximum')
if [ -n "$offenders" ]; then
    printf '%s\n' "$offenders" | while read -r score name line; do
        echo >&2 "app complexity: $name (line $line) scores $score, the maximum is $maximum."
    done
    exit 1
fi

read -r score name _line <<EOF
$(printf '%s\n' "$methods" | sort -rn | head -n 1)
EOF
echo "app complexity: the most complex view-model or store method scores $score ($name); the maximum is $maximum."
