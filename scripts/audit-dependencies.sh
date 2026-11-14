#!/usr/bin/env sh
# Check the locked dependencies against the OSV database of known
# vulnerabilities.
#
# Used by the pre-commit hook and by CI. Only what the Gradle lockfiles pin is
# audited, so a dependency change must regenerate them first:
#   ./gradlew :app:dependencies :core:dependencies --write-locks
set -eu

cd "$(git rev-parse --show-toplevel)"

lockfiles=$(git ls-files 'gradle.lockfile' '*/gradle.lockfile' | sed 's/^/--lockfile=/')
if [ -z "$lockfiles" ]; then
    exit 0
fi

# shellcheck disable=SC2086 # one argument per lockfile
if ! osv-scanner scan source $lockfiles; then
    echo >&2 "The dependency audit failed: a dependency has a known vulnerability, or osv-scanner could not reach its database."
    exit 1
fi
