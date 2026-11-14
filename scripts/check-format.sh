#!/usr/bin/env sh
# Check that every tracked Kotlin source and Gradle build script is formatted.
#
# Used by the pre-commit hook and by CI, so both apply the same style.
# To fix a file: ktfmt --kotlinlang-style <file>
set -eu

cd "$(git rev-parse --show-toplevel)"

kotlin_files=$(git ls-files '*.kt' '*.kts')
if [ -z "$kotlin_files" ]; then
    exit 0
fi

# shellcheck disable=SC2086 # one argument per file; tracked paths contain no spaces
if ! ktfmt --kotlinlang-style --dry-run --set-exit-if-changed $kotlin_files; then
    echo >&2 "The files listed above are not formatted. Run 'ktfmt --kotlinlang-style <file>' on each."
    exit 1
fi
