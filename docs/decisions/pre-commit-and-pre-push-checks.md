# Pre-commit and Pre-push Checks

Decision: `scripts/pre-commit-checks.sh` runs before every commit, in this order:

1. `betterleaks git --pre-commit --staged --redact` — secrets in the staged changes
2. `scripts/check-format.sh` — ktfmt over every tracked Kotlin file and build script
3. `scripts/check-app-is-wiring.sh` — the app module holds only wiring; see `app-module-is-wiring.md`
4. `scripts/audit-dependencies.sh` — osv-scanner over the Gradle lockfiles
5. `./gradlew --quiet test lint` — JVM tests and Android Lint

A missing tool blocks the commit. `scripts/install-check-tools.sh` installs pinned, checksummed
copies of ktfmt and osv-scanner; betterleaks is installed separately.

`scripts/pre-push-checks.sh` runs before every push. When the pushed commits change anything
under `app/`, compared with the remote's `main`, it runs `./gradlew createDebugAndroidTestCoverageReport`
and then `scripts/check-device-coverage.sh`, and it blocks the push when no emulator or device is
connected or when line coverage of the app module's handwritten code is below 80 percent
(Room's generated `_Impl.kt` files are left out). It then runs `scripts/check-app-complexity.sh`, which
blocks the push when a view-model or store method is more complex than its maximum; see
[app-module-is-wiring.md](app-module-is-wiring.md). See
[static-analysis-and-coverage-in-ci.md](static-analysis-and-coverage-in-ci.md) for why.

Rationale: Checks run fastest and most serious first, so a commit containing a secret is stopped
before anything is compiled. A missing tool blocks rather than warns because a skipped check
looks the same as a passed one unless someone is reading the hook's output. Device tests take minutes and need an emulator, so they run before a push
rather than before a commit, and only when the module that needs a device has changed. The pure
Kotlin module is fully covered by the JVM tests in the commit hook.

The format and audit scripts are separate files because CI runs the same two scripts.

The betterleaks command line is that of version 1. Version 2 replaces `git --pre-commit
--staged` with reading a diff on standard input; change the hook when upgrading.
