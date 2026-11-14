#!/usr/bin/env sh
# Tests for scripts/check-commit-msg.sh — the commit-msg hook's check that a
# message is one Conventional Commits line with no body.
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../scripts" && pwd)
checker="$script_dir/check-commit-msg.sh"

# The checker asks git for the repository root and for merge state, so each case
# runs inside a throwaway repository that holds a copy of the scripts.
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
git init -q "$work"
mkdir "$work/scripts"
cp -f "$script_dir/check-commit-msg.sh" "$script_dir/check-commit-subject.sh" "$work/scripts/"
cd "$work"

failures=0

accepts() {
    printf '%b' "$2" > message
    if scripts/check-commit-msg.sh message >/dev/null 2>&1; then
        printf 'ok     accepts: %s\n' "$1"
    else
        printf 'FAILED should accept: %s\n' "$1"
        failures=$((failures + 1))
    fi
}

rejects() {
    printf '%b' "$2" > message
    if scripts/check-commit-msg.sh message >/dev/null 2>&1; then
        printf 'FAILED should reject: %s\n' "$1"
        failures=$((failures + 1))
    else
        printf 'ok     rejects: %s\n' "$1"
    fi
}

accepts "a single conventional line" "feat: log a drink with one tap\n"
accepts "git comment lines after the subject" "feat: log a drink with one tap\n# Please enter the commit message\n"
accepts "blank lines before the subject" "\n\nfeat: log a drink with one tap\n"
accepts "trailing blank lines" "feat: log a drink with one tap\n\n\n"
accepts "a fixup commit" "fixup! feat: log a drink with one tap\n"
accepts "a squash commit" "squash! feat: log a drink with one tap\n"

rejects "a subject that is not conventional" "log a drink\n"
rejects "a body" "feat: log a drink with one tap\n\nThis explains why.\n"
rejects "a trailer" "feat: log a drink with one tap\n\nCo-authored-by: Someone <someone@example.com>\n"
rejects "an empty message" "\n"

# A merge in progress is recognised from repository state, not from the text.
: > .git/MERGE_HEAD
accepts "a merge message while a merge is in progress" "Merge branch 'main' into feat/x\n\n# Conflicts:\n"
rm -f .git/MERGE_HEAD
rejects "a merge-style message with no merge in progress" "Merge branch 'main' into feat/x\n"

if [ "$failures" -ne 0 ]; then
    printf '\n%s test(s) failed\n' "$failures" >&2
    exit 1
fi

printf '\nAll commit message tests passed\n'
