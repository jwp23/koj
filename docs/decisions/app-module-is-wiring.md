# The App Module Is Wiring

Decision: The app module holds only wiring: Room rows and DAOs, stores, view models, screens, and
the few files that tie them together. `scripts/check-app-is-wiring.sh` runs before every commit
and in CI and refuses a Kotlin file in the app module that is not one of those kinds, an import
of `kotlin.math`, and, outside the storage package, an import of the database library or of a
row or DAO.

Rationale: SonarQube's coverage measure excludes the app module because its tests run on a
device, so anything placed there escapes the quality gate that applies to the pure Kotlin
module. Logic that needs no Android type is therefore kept in `:core`. Whether a module is
"thin" cannot be measured directly; this check refuses the visible signs that logic has moved
in. A calculation that looks simple and sits inside a view model still has to be caught in
review.

Adding a file name to the script's list is a decision: the pull request says why that file
cannot live in `:core`.

## Complexity of view models and stores

`scripts/check-app-complexity.sh` runs in the pre-push hook after the coverage check. It reads
the cyclomatic complexity of every method from the device-test coverage report and refuses a
push when a method in a `*ViewModel.kt` or `*Store.kt` file, including the classes the compiler
makes for its lambdas, scores more than the maximum. A method that only passes values between a
store and a screen scores very little, so a high score is the shadow of logic that belongs in
`:core`. This sees what the file-kind check cannot.

Composables are left out because the Compose compiler adds branches of its own to them, and
Room's generated classes because nobody writes them.

The maximum is 6, set on 2026-11-17. It is the highest score measured that day
(`SettingsViewModel$save$1.invokeSuspend`), so nothing written so far fails. Lower it when the
code allows; raise it only with a reason stated in the pull request.

The score is the complexity of the compiled bytecode, so the branches of an inlined call such as
`check` or a coroutine's generated state machine count too. `--list` in place of the maximum
shows the current score of every method measured.
