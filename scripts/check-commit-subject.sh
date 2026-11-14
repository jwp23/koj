#!/usr/bin/env sh
# Validate a single Conventional Commits subject line.
#
# Used by the commit-msg hook for local commits and by CI for pull request
# titles. Because merges are squashed, the pull request title is what lands in
# main's history, so both entry points share this one definition.
#
# Usage: check-commit-subject.sh "<subject>"
set -eu

subject=${1-}

types='feat|fix|chore|docs|refactor|test|perf|ci|style|build|revert'

if printf '%s' "$subject" | grep -Eq "^($types)(\([a-z0-9._/-]+\))?!?: .+"; then
    exit 0
fi

cat >&2 <<MESSAGE
Commit subject does not follow the project convention:

    $subject

Expected a single Conventional Commits line:

    <type>: <description>
    <type>(<scope>): <description>

Valid types: feat, fix, chore, docs, refactor, test, perf, ci, style, build, revert.
Append ! before the colon for a breaking change.

Examples:
    feat: log a drink with one tap
    fix: keep reminders after a device restart
    feat(backup)!: drop the version 1 format
MESSAGE
exit 1
