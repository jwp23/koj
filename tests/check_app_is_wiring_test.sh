#!/usr/bin/env sh
# Tests for scripts/check-app-is-wiring.sh: the app module may hold only the
# known kinds of wiring file, and wiring may not import what only logic or
# storage needs.
#
# Each case runs in a throwaway repository.
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

main=app/src/main/kotlin/io/github/jwp23/koj

# new_repo <name> — an empty repository with the script under test. Leaves the shell inside it.
new_repo() {
    repo=$work/$1
    git init -q -b main "$repo"
    cd "$repo"
    mkdir scripts
    cp -f "$script_dir/check-app-is-wiring.sh" scripts/
}

# add_file <path> [line...] — a tracked Kotlin file holding the given lines.
add_file() {
    path=$1
    shift
    mkdir -p "$(dirname "$path")"
    printf 'package io.github.jwp23.koj\n' > "$path"
    for line in "$@"; do
        printf '%s\n' "$line" >> "$path"
    done
    git add "$path"
}

# allows <description> — the check passes in the current repository.
allows() {
    if output=$(scripts/check-app-is-wiring.sh 2>&1); then
        pass "$1"
    else
        fail "$1: $output"
    fi
}

# refuses <description> <text the message must contain>
refuses() {
    if output=$(scripts/check-app-is-wiring.sh 2>&1); then
        fail "$1: accepted"
    elif printf '%s' "$output" | grep -q "$2"; then
        pass "$1"
    else
        fail "$1: $output"
    fi
}

new_repo empty
allows "a repository with no app code passes"

new_repo kinds
add_file "$main/ui/today/TodayScreen.kt"
add_file "$main/ui/today/TodayViewModel.kt"
add_file "$main/storage/SourceStore.kt"
add_file "$main/storage/SourceRow.kt"
add_file "$main/storage/SourceDao.kt"
allows "screens, view models, stores, rows, and DAOs are wiring"

new_repo named
add_file "$main/KojApplication.kt"
add_file "$main/AppContainer.kt"
add_file "$main/MainActivity.kt"
add_file "$main/ui/KojApp.kt"
add_file "$main/ui/Destinations.kt"
add_file "$main/ui/Ticks.kt"
add_file "$main/ui/graph/DayGraphView.kt"
add_file "$main/ui/PickerFields.kt"
add_file "$main/storage/KojDatabase.kt"
add_file "$main/storage/Converters.kt"
allows "the named wiring files are allowed"

new_repo calculator
add_file "$main/ui/today/CaffeineCalculator.kt"
refuses "a file of another kind is refused" "CaffeineCalculator.kt is not a screen"

new_repo bare-kind
add_file "$main/ui/Screen.kt"
refuses "a kind needs a name in front of it" "ui/Screen.kt is not a screen"

new_repo two-offenders
add_file "$main/Utils.kt"
add_file "$main/ui/Validator.kt"
if output=$(scripts/check-app-is-wiring.sh 2>&1); then
    fail "every offending file is named: accepted"
elif printf '%s' "$output" | grep -q "Utils.kt" && printf '%s' "$output" | grep -q "Validator.kt"; then
    pass "every offending file is named"
else
    fail "every offending file is named: $output"
fi

new_repo math
add_file "$main/ui/today/TodayViewModel.kt" "import kotlin.math.exp"
refuses "a calculation import is refused" "imports kotlin.math"

new_repo room-in-screen
add_file "$main/ui/sources/SourcesScreen.kt" "import androidx.room3.Room"
refuses "a screen that opens the database is refused" "uses the database directly"

new_repo room-in-storage
add_file "$main/storage/SourceDao.kt" "import androidx.room3.Dao" "import androidx.sqlite.driver.AndroidSQLiteDriver"
allows "the storage package may use the database"

new_repo dao-in-view-model
add_file "$main/ui/sources/SourcesViewModel.kt" "import io.github.jwp23.koj.storage.SourceDao"
refuses "a view model that imports a DAO is refused" "imports a row or DAO"

new_repo row-in-screen
add_file "$main/ui/sources/SourcesScreen.kt" "import io.github.jwp23.koj.storage.SourceRow"
refuses "a screen that imports a row is refused" "imports a row or DAO"

new_repo wildcard
add_file "$main/ui/sources/SourcesViewModel.kt" "import io.github.jwp23.koj.storage.*"
refuses "importing the whole storage package is refused" "imports a row or DAO"

new_repo store-in-view-model
add_file "$main/ui/sources/SourcesViewModel.kt" "import io.github.jwp23.koj.storage.SourceStore"
add_file "$main/AppContainer.kt" "import io.github.jwp23.koj.storage.KojDatabase" "import io.github.jwp23.koj.storage.SourceStore"
allows "stores and the database class may be imported outside storage"

new_repo tests-and-core
add_file "app/src/androidTest/kotlin/io/github/jwp23/koj/testing/MutableClock.kt" "import kotlin.math.abs"
add_file "core/src/main/kotlin/io/github/jwp23/koj/intake/Kinetics.kt" "import kotlin.math.exp"
allows "only the app module's main code is checked"

new_repo untracked
mkdir -p "$main"
printf 'package io.github.jwp23.koj\n' > "$main/Scratch.kt"
allows "an untracked file is not checked"

new_repo subdirectory
add_file "$main/Utils.kt"
mkdir -p docs
if output=$(cd docs && ../scripts/check-app-is-wiring.sh 2>&1); then
    fail "the check works from a subdirectory: accepted"
else
    pass "the check works from a subdirectory"
fi

if [ "$failures" -ne 0 ]; then
    printf '%s test(s) failed\n' "$failures"
    exit 1
fi
