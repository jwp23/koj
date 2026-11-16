#!/usr/bin/env sh
# Project quality checks run before every commit.
#
# Invoked by .githooks/pre-commit (contributors) and by .beads/hooks/pre-commit
# (maintainers running the beads issue tracker). Both entry points call this one
# script so the checks cannot drift apart.
#
# Checks are ordered fastest and most critical first. Every tool is required: a
# missing one blocks the commit rather than letting it through unchecked.

root=$(git rev-parse --show-toplevel) || exit 1
cd "$root" || exit 1

# Stop with install advice when a required tool is not on PATH.
require_tool() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo >&2 "pre-commit: $1 is not installed, so the commit cannot be checked. $2"
        exit 1
    fi
}

require_tool betterleaks "Install it from https://github.com/betterleaks/betterleaks"
require_tool ktfmt "Run scripts/install-check-tools.sh"
require_tool osv-scanner "Run scripts/install-check-tools.sh"

if ! betterleaks git --pre-commit --staged --redact; then
    echo >&2 "pre-commit: betterleaks detected secrets in staged changes."
    exit 1
fi

if ! scripts/check-format.sh; then
    echo >&2 "pre-commit: format check failed."
    exit 1
fi

if ! scripts/check-app-is-wiring.sh; then
    echo >&2 "pre-commit: the app module holds something that is not wiring."
    exit 1
fi

if ! scripts/audit-dependencies.sh; then
    echo >&2 "pre-commit: dependency audit failed."
    exit 1
fi

if ! ./gradlew --quiet --no-configuration-cache test lint; then
    echo >&2 "pre-commit: tests or lint failed."
    exit 1
fi
