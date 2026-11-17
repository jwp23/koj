#!/usr/bin/env sh
# Tests for scripts/check-app-complexity.sh: a push is refused when a view model
# or store method is more complex than the maximum.
set -eu

script=$(CDPATH= cd -- "$(dirname -- "$0")/../scripts" && pwd)/check-app-complexity.sh

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

failures=0
pass() { printf 'ok     %s\n' "$1"; }
fail() {
    printf 'FAILED %s\n' "$1"
    failures=$((failures + 1))
}

# method <name> <missed> <covered>: one method as JaCoCo writes it, with its
# COMPLEXITY counter between an INSTRUCTION and a LINE counter.
method() {
    printf '<method name="%s" desc="()V" line="42"><counter type="INSTRUCTION" missed="1" covered="1"/><counter type="COMPLEXITY" missed="%s" covered="%s"/><counter type="LINE" missed="0" covered="1"/></method>' "$1" "$2" "$3"
}

# class <name> <source file> <name:missed:covered>...: one class with its methods
# and, after them, class-level counters whose COMPLEXITY is deliberately large.
class() {
    name=$1
    source=$2
    shift 2
    printf '<class name="io/github/jwp23/koj/ui/today/%s" sourcefilename="%s">' "$name" "$source"
    for triple in "$@"; do
        old_ifs=$IFS
        IFS=:
        # shellcheck disable=SC2086
        set -- $triple
        IFS=$old_ifs
        method "$1" "$2" "$3"
    done
    printf '<counter type="INSTRUCTION" missed="9" covered="9"/><counter type="COMPLEXITY" missed="100" covered="100"/><counter type="LINE" missed="0" covered="9"/></class>'
}

# report <classes...>: a whole report, one line, with package and report counters.
report() {
    printf '<?xml version="1.0" encoding="UTF-8"?><report name="debug"><package name="io/github/jwp23/koj/ui/today">%s<counter type="COMPLEXITY" missed="100" covered="100"/></package><counter type="COMPLEXITY" missed="100" covered="100"/></report>' "$*"
}

# check <description> <expected exit> <expected text> <report content> [maximum]
check() {
    printf '%s' "$4" > "$work/report.xml"
    status=0
    output=$("$script" "$work/report.xml" ${5:+"$5"} 2>&1) || status=$?
    if [ "$status" -eq "$2" ] && printf '%s' "$output" | grep -qF -- "$3"; then
        pass "$1"
    else
        fail "$1 (exit $status: $output)"
    fi
}

check "a simple view model passes" 0 "scores 3" \
    "$(report "$(class TodayViewModel TodayViewModel.kt a:0:1 b:1:2)")" 4
check "a method over the maximum is refused" 1 "TodayViewModel.recompute" \
    "$(report "$(class TodayViewModel TodayViewModel.kt recompute:2:5)")" 4
check "the refusal names the score and the maximum" 1 "scores 7, the maximum is 4" \
    "$(report "$(class TodayViewModel TodayViewModel.kt recompute:2:5)")" 4
check "a method at the maximum passes" 0 "the maximum is 4." \
    "$(report "$(class TodayViewModel TodayViewModel.kt a:1:3)")" 4
check "every offending method is named" 1 "TodayViewModel.second" \
    "$(report "$(class TodayViewModel TodayViewModel.kt first:0:9 second:0:8)")" 4
check "every offending method is named (the first too)" 1 "TodayViewModel.first" \
    "$(report "$(class TodayViewModel TodayViewModel.kt first:0:9 second:0:8)")" 4
check "stores are checked" 1 "DrinkStore.add" \
    "$(report "$(class DrinkStore DrinkStore.kt add:3:6)")" 4
check "screens are not checked" 0 "no view-model or store methods" \
    "$(report "$(class TodayScreenKt TodayScreen.kt draw:20:20)")" 4
check "generated database classes are not checked" 0 "no view-model or store methods" \
    "$(report "$(class SourceDao_Impl SourceDao_Impl.kt query:10:20)")" 4
check "a class's own total is not mistaken for a method" 0 "scores 2" \
    "$(report "$(class TodayViewModel TodayViewModel.kt a:1:1)")" 4
check "a lambda's class is checked with its file" 1 'TodayViewModel$state$1.invokeSuspend' \
    "$(report "$(class 'TodayViewModel$state$1' TodayViewModel.kt invokeSuspend:3:5)")" 4
check "a report with no view models or stores passes and says so" 0 "no view-model or store methods" \
    "$(report "$(class TodayScreenKt TodayScreen.kt draw:1:1)")"

check "the default maximum admits a method scoring 6" 0 "the maximum is 6." \
    "$(report "$(class TodayViewModel TodayViewModel.kt a:0:6)")"
check "the default maximum refuses a method scoring 7" 1 "scores 7, the maximum is 6" \
    "$(report "$(class TodayViewModel TodayViewModel.kt a:0:7)")"

printf '%s' "$(report "$(class TodayViewModel TodayViewModel.kt a:0:1 b:0:5 c:0:3)")" > "$work/report.xml"
status=0
output=$("$script" "$work/report.xml" --list 2>&1) || status=$?
expected=$(printf '5 io.github.jwp23.koj.ui.today.TodayViewModel.b\n3 io.github.jwp23.koj.ui.today.TodayViewModel.c\n1 io.github.jwp23.koj.ui.today.TodayViewModel.a')
if [ "$status" -eq 0 ] && [ "$output" = "$expected" ]; then
    pass "--list prints every considered method, highest first"
else
    fail "--list prints every considered method, highest first (exit $status: $output)"
fi

status=0
output=$("$script" "$work/missing.xml" 2>&1) || status=$?
if [ "$status" -eq 1 ] && printf '%s' "$output" | grep -qF "no report at"; then
    pass "a missing report is refused"
else
    fail "a missing report is refused (exit $status: $output)"
fi

if [ "$failures" -gt 0 ]; then
    printf '%s case(s) failed\n' "$failures"
    exit 1
fi
