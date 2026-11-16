#!/usr/bin/env sh
# Tests for scripts/pre-push-checks.sh: a push that changes the app module must
# pass the device tests, and is blocked when no device can run them.
#
# Each case runs in a throwaway repository with stand-ins for adb and the
# Gradle wrapper, so the test needs neither an Android SDK nor a device. The
# stand-in wrapper records how it was called.
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../scripts" && pwd)

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

failures=0

pass() { printf 'ok     %s\n' "$1"; }

fail() {
    printf 'FAILED %s\n' "$1"
    failures=$((failures + 1))
}

# new_repo <name> — a repository on branch main with one commit, a remote-tracking
# ref for main, and copies of the scripts under test. Leaves the shell inside it.
new_repo() {
    repo=$work/$1
    git init -q -b main "$repo"
    cd "$repo"
    git config user.name "Test Author"
    git config user.email "author@example.com"
    mkdir scripts
    cp -f "$script_dir/pre-push-checks.sh" "$script_dir/changes-need-device-tests.sh" "$script_dir/check-device-coverage.sh" "$script_dir/check-app-complexity.sh" scripts/
    printf 'notes\n' > README.md
    git add .
    git commit -q -m "chore: start"
    git update-ref refs/remotes/origin/main HEAD
    git checkout -q -b feat/change
}

# commit_file <path> — commit a new file on the current branch.
commit_file() {
    mkdir -p "$(dirname "$1")"
    printf 'content\n' > "$1"
    git add "$1"
    git commit -q -m "feat: add $1"
}

# with_sdk <adb devices output> [missed] [covered] [complexity] — an SDK whose adb
# prints the given device list, and a Gradle wrapper that records its arguments,
# writes a coverage report with the given line totals and one view-model method
# of the given complexity (default 1), and succeeds.
with_sdk() {
    mkdir -p sdk/platform-tools
    printf '#!/bin/sh\nprintf "%%b" "%s"\n' "$1" > sdk/platform-tools/adb
    chmod +x sdk/platform-tools/adb
    printf 'sdk.dir=%s\n' "$PWD/sdk" > local.properties
    report_xml=$(printf '<report name="debug"><package name="p"><class name="p/TodayViewModel" sourcefilename="TodayViewModel.kt"><method name="finish" desc="()V" line="1"><counter type="COMPLEXITY" missed="0" covered="%s"/></method></class><sourcefile name="Screen.kt"><counter type="LINE" missed="%s" covered="%s"/></sourcefile></package></report>' "${4:-1}" "${2:-10}" "${3:-90}")
    cat > gradlew <<EOF
#!/bin/sh
echo "\$@" > gradle-was-called
mkdir -p app/build/reports/coverage/androidTest/debug/connected
printf '%s' '$report_xml' > app/build/reports/coverage/androidTest/debug/connected/report.xml
EOF
    chmod +x gradlew
}

# push_branch — run the hook as git would for a push of the current branch.
push_branch() {
    printf 'refs/heads/feat/change %s refs/heads/feat/change %s\n' \
        "$(git rev-parse HEAD)" "$(git rev-parse HEAD | tr '0-9a-f' '0')" |
        env -u ANDROID_HOME scripts/pre-push-checks.sh origin 2>&1
}

new_repo docs-only
commit_file docs/notes.md
if output=$(push_branch) && [ ! -e gradle-was-called ]; then
    pass "a push that leaves the app module alone needs no device"
else
    fail "a push that leaves the app module alone needs no device: $output"
fi

new_repo no-sdk
commit_file app/src/main/kotlin/Screen.kt
if output=$(push_branch); then
    fail "an app change without an Android SDK is blocked"
elif printf '%s' "$output" | grep -q "pre-push: no Android SDK found"; then
    pass "an app change without an Android SDK is blocked"
else
    fail "an app change without an Android SDK is blocked: $output"
fi

new_repo no-device
commit_file app/src/main/kotlin/Screen.kt
with_sdk 'List of devices attached\n\n'
if output=$(push_branch); then
    fail "an app change with no device connected is blocked"
elif printf '%s' "$output" | grep -q "pre-push: no emulator or device is connected"; then
    pass "an app change with no device connected is blocked"
else
    fail "an app change with no device connected is blocked: $output"
fi

new_repo offline-device
commit_file app/src/main/kotlin/Screen.kt
with_sdk 'List of devices attached\nemulator-5554\toffline\n\n'
if output=$(push_branch); then
    fail "a device that is still starting does not count"
else
    pass "a device that is still starting does not count"
fi

new_repo ready-device
commit_file app/src/main/kotlin/Screen.kt
with_sdk 'List of devices attached\nemulator-5554\tdevice\n\n'
if output=$(push_branch) && [ "$(cat gradle-was-called)" = "--quiet createDebugAndroidTestCoverageReport" ]; then
    pass "an app change with a device connected runs the device tests"
else
    fail "an app change with a device connected runs the device tests: $output"
fi

new_repo low-device-coverage
commit_file app/src/main/kotlin/Screen.kt
with_sdk 'List of devices attached\nemulator-5554\tdevice\n\n' 30 70
if output=$(push_branch); then
    fail "device tests that cover too little block the push"
elif printf '%s' "$output" | grep -q "device coverage:" && printf '%s' "$output" | grep -q "70%"; then
    pass "device tests that cover too little block the push"
else
    fail "device tests that cover too little block the push: $output"
fi

new_repo enough-device-coverage
commit_file app/src/main/kotlin/Screen.kt
with_sdk 'List of devices attached\nemulator-5554\tdevice\n\n' 10 90
if output=$(push_branch); then
    pass "device tests that cover enough allow the push"
else
    fail "device tests that cover enough allow the push: $output"
fi

new_repo too-complex-view-model
commit_file app/src/main/kotlin/Screen.kt
with_sdk 'List of devices attached\nemulator-5554\tdevice\n\n' 10 90 99
if output=$(push_branch); then
    fail "a view model that is too complex blocks the push"
elif printf '%s' "$output" | grep -q "app complexity:"; then
    pass "a view model that is too complex blocks the push"
else
    fail "a view model that is too complex blocks the push: $output"
fi

new_repo failing-device-tests
commit_file app/src/main/kotlin/Screen.kt
with_sdk 'List of devices attached\nemulator-5554\tdevice\n\n'
printf '#!/bin/sh\nexit 1\n' > gradlew
if output=$(push_branch); then
    fail "failing device tests block the push"
elif printf '%s' "$output" | grep -q "pre-push: device tests failed"; then
    pass "failing device tests block the push"
else
    fail "failing device tests block the push: $output"
fi

new_repo no-remote-main
git update-ref -d refs/remotes/origin/main
commit_file docs/notes.md
mkdir -p app && printf 'x\n' > app/existing.txt && git add app && git commit -q -m "feat: add app"
git checkout -q -b feat/docs-after-app
commit_file docs/more.md
with_sdk 'List of devices attached\nemulator-5554\tdevice\n\n'
if output=$(push_branch) && [ -e gradle-was-called ]; then
    pass "with no remote main to compare against, every file counts as changed"
else
    fail "with no remote main to compare against, every file counts as changed: $output"
fi

new_repo delete-branch
commit_file app/src/main/kotlin/Screen.kt
zero=$(git rev-parse HEAD | tr '0-9a-f' '0')
if output=$(printf '(delete) %s refs/heads/feat/change %s\n' "$zero" "$(git rev-parse HEAD)" |
    env -u ANDROID_HOME scripts/pre-push-checks.sh origin 2>&1); then
    pass "deleting a remote branch needs no device"
else
    fail "deleting a remote branch needs no device: $output"
fi

new_repo rename-out-of-app
commit_file app/src/main/res/ic_launcher.xml
git checkout -q -b feat/move
mkdir -p core && git mv app/src/main/res/ic_launcher.xml core/ic_launcher.xml
git commit -q -m "refactor: move launcher icon"
git update-ref refs/remotes/origin/main "$(git rev-parse HEAD~1)"
with_sdk 'List of devices attached\n\n'
if output=$(push_branch); then
    fail "moving a file out of the app module is blocked without a device"
elif printf '%s' "$output" | grep -q "pre-push: no emulator or device is connected"; then
    pass "moving a file out of the app module is blocked without a device"
else
    fail "moving a file out of the app module is blocked without a device: $output"
fi

new_repo non-ascii-path
commit_file "app/src/main/kotlin/Caf$(printf '\303\251').kt"
with_sdk 'List of devices attached\n\n'
if output=$(push_branch); then
    fail "an app file with a non-ASCII name is blocked without a device"
elif printf '%s' "$output" | grep -q "pre-push: no emulator or device is connected"; then
    pass "an app file with a non-ASCII name is blocked without a device"
else
    fail "an app file with a non-ASCII name is blocked without a device: $output"
fi

new_repo special-char-path
commit_file "app/src/main/kotlin/tab$(printf '\t')name.kt"
with_sdk 'List of devices attached\n\n'
if output=$(push_branch); then
    fail "an app file with a tab in its name is blocked without a device"
else
    pass "an app file with a tab in its name is blocked without a device"
fi

new_repo non-executable-rule
commit_file docs/notes.md
chmod -x scripts/changes-need-device-tests.sh
if output=$(push_branch); then
    fail "a rule script that cannot run blocks the push"
elif printf '%s' "$output" | grep -q "pre-push: could not decide whether device tests are needed"; then
    pass "a rule script that cannot run blocks the push"
else
    fail "a rule script that cannot run blocks the push: $output"
fi

new_repo failing-rule
commit_file docs/notes.md
printf '#!/bin/sh\nexit 2\n' > scripts/changes-need-device-tests.sh
if output=$(push_branch); then
    fail "a rule script that fails unexpectedly blocks the push"
elif printf '%s' "$output" | grep -q "pre-push: could not decide whether device tests are needed"; then
    pass "a rule script that fails unexpectedly blocks the push"
else
    fail "a rule script that fails unexpectedly blocks the push: $output"
fi

new_repo failing-diff
commit_file docs/notes.md
if output=$(printf 'refs/heads/feat/change %s refs/heads/feat/change %s\n' \
    "$(git rev-parse HEAD | tr '0-9a-f' '1')" "$(git rev-parse HEAD | tr '0-9a-f' '0')" |
    env -u ANDROID_HOME scripts/pre-push-checks.sh origin 2>&1); then
    fail "a failing git diff blocks the push"
else
    pass "a failing git diff blocks the push"
fi

if [ "$failures" -ne 0 ]; then
    printf '\n%s test(s) failed\n' "$failures" >&2
    exit 1
fi

printf '\nAll pre-push check tests passed\n'
