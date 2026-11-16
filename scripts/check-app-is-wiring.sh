#!/usr/bin/env sh
# Keep the app module to wiring: stored rows, stores, view models, and screens.
#
# Logic that needs no Android type belongs in the pure Kotlin module, where it
# is tested on the JVM and counted by the coverage gate. This check cannot
# measure "thin"; it refuses the two things that show logic has moved in: a
# file that is not one of the known kinds, and an import that wiring has no
# use for. See docs/decisions/app-module-is-wiring.md.
#
# Usage: check-app-is-wiring.sh   (run anywhere inside the repository)
# Checks every tracked Kotlin file under app/src/main/kotlin. Exits 1 and
# names each offending file when the rule is broken.
set -eu

cd "$(git rev-parse --show-toplevel)"

main=app/src/main/kotlin

# Kinds of file, by the end of the name.
allowed_kinds='Screen.kt ViewModel.kt Store.kt Row.kt Dao.kt'
# Single files that are wiring but fit no kind. Adding a name here is a
# decision: say in the pull request why the file cannot live in :core.
allowed_files='KojApplication.kt AppContainer.kt MainActivity.kt KojApp.kt Destinations.kt Ticks.kt KojDatabase.kt Converters.kt DayGraphView.kt PickerFields.kt'

failed=0

report() {
    echo >&2 "app is wiring: $1"
    failed=1
}

is_allowed_name() {
    for kind in $allowed_kinds; do
        case "$1" in
            ?*"$kind") return 0 ;;
        esac
    done
    for file in $allowed_files; do
        [ "$1" = "$file" ] && return 0
    done
    return 1
}

files=$(mktemp)
trap 'rm -f "$files"' EXIT
git ls-files -- "$main" | grep '\.kt$' > "$files" || true

while IFS= read -r path; do
    name=${path##*/}
    if ! is_allowed_name "$name"; then
        report "$path is not a screen, view model, store, row, or DAO. Logic belongs in :core; wiring files are named *Screen.kt, *ViewModel.kt, *Store.kt, *Row.kt, or *Dao.kt."
    fi

    if grep -q '^import kotlin\.math\.' "$path"; then
        report "$path imports kotlin.math. Calculations belong in :core."
    fi

    case "$path" in
        */storage/*) ;;
        *)
            if grep -q -e '^import androidx\.room3\.' -e '^import androidx\.sqlite\.' "$path"; then
                report "$path uses the database directly. Only the storage package may; everything else goes through a store."
            fi
            if grep -q -e '^import io\.github\.jwp23\.koj\.storage\..*\(Row\|Dao\)$' -e '^import io\.github\.jwp23\.koj\.storage\.\*$' "$path"; then
                report "$path imports a row or DAO. Outside the storage package, use the stores and the records from :core."
            fi
            ;;
    esac
done < "$files"

exit "$failed"
