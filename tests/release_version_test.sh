#!/usr/bin/env sh
# Tests for scripts/release-version.sh: a release tag becomes a version name
# and a version code, and anything that is not a release tag is refused.
set -eu

script=$(CDPATH= cd -- "$(dirname -- "$0")/../scripts" && pwd)/release-version.sh

failures=0

pass() { printf 'ok     %s\n' "$1"; }

fail() {
    printf 'FAILED %s\n' "$1"
    failures=$((failures + 1))
}

# gives <tag> <version name> <version code> <description>
gives() {
    expected=$(printf 'versionName=%s\nversionCode=%s' "$2" "$3")
    if output=$("$script" "$1" 2>&1) && [ "$output" = "$expected" ]; then
        pass "$4"
    else
        fail "$4: $output"
    fi
}

# refuses <tag> <description>
refuses() {
    if output=$("$script" "$1" 2>&1); then
        fail "$2: accepted as $output"
    elif printf '%s' "$output" | grep -q "^release-version: "; then
        pass "$2"
    else
        fail "$2: $output"
    fi
}

# 1 * 10000 + 4 * 100 + 2 = 10402
gives v1.4.2 1.4.2 10402 "a tag becomes its name and a code built from its three numbers"
# 0 * 10000 + 1 * 100 + 0 = 100
gives v0.1.0 0.1.0 100 "a version below 1.0.0 has a code above zero once minor or patch is set"
# 0 * 10000 + 99 * 100 + 99 = 9999, one below 1.0.0's 10000
gives v0.99.99 0.99.99 9999 "the largest minor and patch stay below the next major version"
gives v1.0.0 1.0.0 10000 "a new major version has a higher code than any earlier release"
# 12 * 10000 + 0 * 100 + 7 = 120007
gives v12.0.7 12.0.7 120007 "a major version may have several digits"

refuses 1.4.2 "a tag without the leading v is refused"
refuses v1.4 "a tag with two numbers is refused"
refuses v1.4.2.1 "a tag with four numbers is refused"
refuses v1.100.0 "a minor version above 99 is refused"
refuses v1.0.100 "a patch version above 99 is refused"
refuses v01.2.3 "a number with a leading zero is refused"
refuses v1.2.3-beta "a tag with a suffix is refused"
refuses "" "an empty tag is refused"
refuses "v1.2.3
v9.9.9" "a tag containing a line break is refused"
refuses v210000.0.0 "a major version too large for a version code is refused"

if [ "$failures" -ne 0 ]; then
    printf '%s test(s) failed\n' "$failures"
    exit 1
fi
