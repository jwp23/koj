#!/usr/bin/env sh
# Tests for scripts/pre-commit-checks.sh: a missing check tool must block the
# commit. The checks themselves belong to the tools; what is tested here is
# that the script never skips one silently.
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../scripts" && pwd)
checks="$script_dir/pre-commit-checks.sh"

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
git init -q "$work/repo"
cd "$work/repo"

# A PATH holding git and the shell utilities the script needs, plus whichever
# stand-in tools a case provides. The stand-ins succeed without doing anything.
mkdir "$work/base"
for tool in git sh sed; do
    ln -s "$(command -v "$tool")" "$work/base/$tool"
done

# stand_in <directory> [tool...] — create the directory holding the named tools.
stand_in() {
    tools_dir=$work/$1
    shift
    mkdir -p "$tools_dir"
    for tool in "$@"; do
        printf '#!/bin/sh\nexit 0\n' > "$tools_dir/$tool"
        chmod +x "$tools_dir/$tool"
    done
}

failures=0

blocks_without() {
    missing=$1
    tools_dir=$2
    if output=$(PATH="$work/$tools_dir:$work/base" "$checks" 2>&1); then
        printf 'FAILED should block without %s\n' "$missing"
        failures=$((failures + 1))
    elif printf '%s' "$output" | grep -q "pre-commit: $missing is not installed"; then
        printf 'ok     blocks without %s\n' "$missing"
    else
        printf 'FAILED wrong message without %s: %s\n' "$missing" "$output"
        failures=$((failures + 1))
    fi
}

stand_in none
stand_in no-ktfmt betterleaks osv-scanner
stand_in no-osv-scanner betterleaks ktfmt

blocks_without betterleaks none
blocks_without ktfmt no-ktfmt
blocks_without osv-scanner no-osv-scanner

if [ "$failures" -ne 0 ]; then
    printf '\n%s test(s) failed\n' "$failures" >&2
    exit 1
fi

printf '\nAll pre-commit check tests passed\n'
