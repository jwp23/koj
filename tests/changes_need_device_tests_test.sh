#!/usr/bin/env sh
# Tests for scripts/changes-need-device-tests.sh — the rule deciding whether a
# push must run the device tests first.
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../scripts" && pwd)
rule="$script_dir/changes-need-device-tests.sh"

failures=0

needs_device_tests() {
    if printf '%b' "$2" | "$rule"; then
        printf 'ok     needs device tests: %s\n' "$1"
    else
        printf 'FAILED should need device tests: %s\n' "$1"
        failures=$((failures + 1))
    fi
}

skips_device_tests() {
    if printf '%b' "$2" | "$rule"; then
        printf 'FAILED should skip device tests: %s\n' "$1"
        failures=$((failures + 1))
    else
        printf 'ok     skips device tests: %s\n' "$1"
    fi
}

needs_device_tests "an app source file" "app/src/main/kotlin/io/github/jwp23/koj/MainActivity.kt\n"
needs_device_tests "an app device test" "app/src/androidTest/kotlin/io/github/jwp23/koj/MainActivityTest.kt\n"
needs_device_tests "the app build script" "app/build.gradle.kts\n"
needs_device_tests "the app manifest" "app/src/main/AndroidManifest.xml\n"
needs_device_tests "an app file among others" "README.md\ncore/build.gradle.kts\napp/gradle.lockfile\n"

skips_device_tests "no changes" ""
skips_device_tests "documentation" "docs/designs/reminders.md\nAGENTS.md\n"
skips_device_tests "the pure Kotlin module" "core/src/main/kotlin/io/github/jwp23/koj/intake/Kinetics.kt\n"
skips_device_tests "a path that only contains app/" "docs/app/notes.md\nscripts/app/tool.sh\n"
skips_device_tests "a file whose name starts with app" "application.md\n"

if [ "$failures" -ne 0 ]; then
    printf '\n%s test(s) failed\n' "$failures" >&2
    exit 1
fi

printf '\nAll device test rule tests passed\n'
