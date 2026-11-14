#!/usr/bin/env sh
# Run the device tests before a push that changes the app module.
#
# Invoked by .githooks/pre-push (contributors) and .beads/hooks/pre-push
# (maintainers running the beads issue tracker) with git's pre-push arguments:
# the remote's name as $1 and, on standard input, one line per ref being
# pushed: <local ref> <local object name> <remote ref> <remote object name>.
#
# The push is blocked when the device tests are needed and no emulator or
# device is connected to run them.
set -eu

remote=$1
cd "$(git rev-parse --show-toplevel)"

# List, NUL-separated, the files each pushed ref changes relative to the remote's
# main branch. NUL-separated output is verbatim, so git does not quote names with
# non-ASCII or special characters. Renames are listed as a delete and an add, so a
# file moved out of app/ still counts as a change under app/. Exits 1 if git fails.
changed_files() {
    while read -r _local_ref local_object _remote_ref _remote_object; do
        # Deleting a remote ref pushes an all-zero object name: nothing to test.
        case "$local_object" in
            *[!0]*) ;;
            *) continue ;;
        esac
        base=$(git merge-base "$local_object" "refs/remotes/$remote/main" 2>/dev/null || true)
        if [ -n "$base" ]; then
            git diff -z --no-renames --name-only "$base" "$local_object" || exit 1
        else
            # Nothing to compare with yet, so every file counts as changed.
            git ls-tree -r -z --name-only "$local_object" || exit 1
        fi
    done
}

block_unverifiable() {
    echo >&2 "pre-push: could not decide whether device tests are needed: $1"
    echo >&2 "The push is blocked rather than assumed safe."
    exit 1
}

changes=$(mktemp)
trap 'rm -f "$changes"' EXIT
changed_files > "$changes" || block_unverifiable "git could not list the changed files."

# The rule exits 0 when device tests are needed and 1 when they are not; any
# other status (missing, not executable, crashed) means it did not decide.
rule_status=0
tr '\0' '\n' < "$changes" | scripts/changes-need-device-tests.sh || rule_status=$?
case "$rule_status" in
    0) ;;
    1) exit 0 ;;
    *) block_unverifiable "scripts/changes-need-device-tests.sh exited with status $rule_status." ;;
esac

sdk=${ANDROID_HOME:-$(sed -n 's/^sdk\.dir=//p' local.properties 2>/dev/null || true)}
adb=$sdk/platform-tools/adb
if [ ! -x "$adb" ]; then
    echo >&2 "pre-push: no Android SDK found, so the device tests cannot run."
    echo >&2 "Set sdk.dir in local.properties or ANDROID_HOME. See docs/device-verification.md."
    exit 1
fi

# A device that is ready is listed as "<serial><tab>device".
if ! "$adb" devices | grep -q "$(printf '\tdevice$')"; then
    echo >&2 "pre-push: no emulator or device is connected, so the device tests cannot run."
    echo >&2 "This push changes the app module. Start an emulator (see docs/device-verification.md) and push again."
    exit 1
fi

if ! ./gradlew --quiet connectedAndroidTest; then
    echo >&2 "pre-push: device tests failed."
    exit 1
fi
