#!/usr/bin/env sh
# Tests for scripts/check-commit-subject.sh — the shared Conventional Commits
# validator used by the commit-msg hook and by CI's pull request title check.
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../scripts" && pwd)
validator="$script_dir/check-commit-subject.sh"

failures=0

accepts() {
    if "$validator" "$1" >/dev/null 2>&1; then
        printf 'ok     accepts: %s\n' "$1"
    else
        printf 'FAILED should accept: %s\n' "$1"
        failures=$((failures + 1))
    fi
}

rejects() {
    if "$validator" "$1" >/dev/null 2>&1; then
        printf 'FAILED should reject: %s\n' "$1"
        failures=$((failures + 1))
    else
        printf 'ok     rejects: %s\n' "$1"
    fi
}

accepts "feat: log a drink with one tap"
accepts "fix: keep reminders after a device restart"
accepts "chore: add the Gradle wrapper"
accepts "docs: describe the hooks setup step"
accepts "refactor: extract dose spreading from the intake model"
accepts "test: cover a sipped drink still in progress"
accepts "perf: reuse the day's curve between redraws"
accepts "ci: pin actions to commit SHAs"
accepts "style: reformat the intake model"
accepts "build: raise the compile SDK"
accepts "revert: undo the daily check-in cap"
accepts "feat(graph): mark check-ins on the time axis"
accepts "fix(intake/model): count drinks from earlier days"
accepts "feat!: change the backup file format"
accepts "feat(backup)!: drop the version 1 format"

rejects "log a drink with one tap"
rejects "Feat: capitalised type"
rejects "feature: not a recognised type"
rejects "feat log a drink"
rejects "feat:"
rejects "feat: "
rejects ""
rejects "wip"
rejects "Merge branch 'main' into feat/x"

if [ "$failures" -ne 0 ]; then
    printf '\n%s test(s) failed\n' "$failures" >&2
    exit 1
fi

printf '\nAll commit subject validator tests passed\n'
