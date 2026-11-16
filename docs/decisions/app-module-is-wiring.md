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
