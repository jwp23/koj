# Storage With Room 3

Decision: Storage uses Room 3 (`androidx.room3`), with its compiler run by KSP, the Room Gradle
plugin exporting schemas to `app/schemas/`, and `AndroidSQLiteDriver` as the SQLite driver. The
app module declares `kotlinx-coroutines-android` itself, at the same version as
`kotlinx-coroutines-test`. Versions live in `gradle/libs.versions.toml`. The checked-in build does not declare any of this
yet; it is added with the first screen that stores data.

Rationale: Room 3 is the current stable line and the only one the Android guides still
document; a new app has nothing that depends on the older API it drops. Room 3 requires KSP and
an explicit driver. `AndroidSQLiteDriver` uses the phone's own SQLite and comes with
`room3-runtime`, so it adds no dependency and no native library to the APK; the SQLite version
still varies by device, because manufacturers can ship different versions for the same Android
API level.

Committed schema files let a later version of the database be tested against an earlier one.

## Coroutines must be declared

Room brings in an older coroutines version than the current `kotlinx-coroutines-test`. The
Android Gradle Plugin aligns the device-test classpath to the app's, so the test library then
runs against a coroutines core it was not built for, and every device test fails at start with
`NoSuchMethodError: No static method runBlockingK$default`. That includes Compose tests that use
no coroutine themselves, because the Compose test rule calls `runTest`. Declaring
`kotlinx-coroutines-android` in the app at the test library's version keeps the two equal. Bump
them together.

## Version choice

Checked 2026-11-14. KSP 2.3.12 is the version Kotlin's KSP documentation names for Kotlin 2.3.20
to 2.4.x, and its release notes include the fixes for the Android Gradle Plugin 9's built-in
Kotlin. KSP 2.3.14 and kotlinx-serialization 1.13.0 were newer but less than seven days old, the
waiting period `.github/dependabot.yml` applies to every version update, so the build pins 2.3.12 and
1.11.0 and Dependabot will propose the newer ones.

The pinned set was built with warnings as errors, linted, audited, and run on an Android 17
emulator, including a minified release build.
