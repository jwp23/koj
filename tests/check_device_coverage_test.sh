#!/usr/bin/env sh
# Tests for scripts/check-device-coverage.sh: a push is refused when the device
# tests cover too little of the app module's handwritten code.
set -eu

script=$(CDPATH= cd -- "$(dirname -- "$0")/../scripts" && pwd)/check-device-coverage.sh

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

failures=0
pass() { printf 'ok     %s\n' "$1"; }
fail() {
    printf 'FAILED %s\n' "$1"
    failures=$((failures + 1))
}

# file <name> <missed> <covered>: one source file as JaCoCo writes it.
file() {
    printf '<sourcefile name="%s"><line nr="1" mi="0" ci="1" mb="0" cb="0"/><counter type="INSTRUCTION" missed="1" covered="1"/><counter type="LINE" missed="%s" covered="%s"/><counter type="METHOD" missed="0" covered="1"/></sourcefile>' "$1" "$2" "$3"
}

# report <source files...>: a whole report, one line, with the class, package,
# and report counters that a real one also carries.
report() {
    printf '<?xml version="1.0" encoding="UTF-8"?><report name="debug"><package name="p"><class name="p/A" sourcefilename="A.kt"><method name="a" desc="()V" line="1"><counter type="LINE" missed="900" covered="0"/></method><counter type="LINE" missed="900" covered="0"/></class><sourcefile name="Emitters.kt"/>%s<counter type="LINE" missed="900" covered="0"/></package><counter type="LINE" missed="900" covered="0"/></report>' "$*"
}

# check <description> <expected exit> <expected text> <report content> [minimum]
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

check "coverage at the minimum passes" 0 "80%" "$(report "$(file Screen.kt 20 80)")"
check "coverage below the minimum is refused" 1 "79%" "$(report "$(file Screen.kt 21 79)")"
check "the refusal names the minimum" 1 "minimum is 80%" "$(report "$(file Screen.kt 21 79)")"
check "a fraction below the minimum does not round up" 1 "79%" "$(report "$(file Screen.kt 201 799)")"
check "every handwritten file is counted" 1 "75% of the app module's handwritten lines (150 of 200)" \
    "$(report "$(file Screen.kt 0 100)$(file Store.kt 50 50)")"
check "code Room generates is not counted" 0 "80% of the app module's handwritten lines (80 of 100)" \
    "$(report "$(file Screen.kt 20 80)$(file SourceDao_Impl.kt 500 0)$(file KojDatabase_Impl.kt 500 0)")"
check "class, package, and report counters are not counted" 0 "(80 of 100)" "$(report "$(file Screen.kt 20 80)")"
check "a different minimum can be given" 0 "50%" "$(report "$(file Screen.kt 50 50)")" 50
check "a report without source files is refused" 1 "has no line totals" '<report name="debug"></report>'
check "a report with only generated code is refused" 1 "has no line totals" "$(report "$(file SourceDao_Impl.kt 1 9)")"
check "a report with no lines at all is refused" 1 "has no line totals" "$(report "$(file Screen.kt 0 0)")"

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
