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

# List the files each pushed ref changes relative to the remote's main branch.
changed_files() {
    while read -r _local_ref local_object _remote_ref _remote_object; do
        # Deleting a remote ref pushes an all-zero object name: nothing to test.
        case "$local_object" in
            *[!0]*) ;;
            *) continue ;;
        esac
        base=$(git merge-base "$local_object" "refs/remotes/$remote/main" 2>/dev/null || true)
        if [ -n "$base" ]; then
            git diff --name-only "$base" "$local_object"
        else
            # Nothing to compare with yet, so every file counts as changed.
            git ls-tree -r --name-only "$local_object"
        fi
    done
}

if ! changed_files | scripts/changes-need-device-tests.sh; then
    exit 0
fi

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
