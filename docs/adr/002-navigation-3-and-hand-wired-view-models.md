# ADR-002: Navigation 3 and Hand-Wired View Models

## Context

The app has five screens: Today, Log a drink, History, Sources, and Settings. Each reads from
storage and most write to it. Something must decide which screen is showing, carry the user back,
keep each screen's state through a rotation or a restart of the process, and hand each screen the
storage it needs.

On 2026-11-14 Google's release notes mark `androidx.navigation` (Navigation Compose) as in
maintenance mode: "will only receive critical fixes; new features are not planned". Navigation 3
(`androidx.navigation3`) has been stable since version 1.0.0 in November 2025 and is "designed
exclusively for Compose".

## Decision

Screens navigate with Navigation 3. The back stack is a list the app owns, created with
`rememberNavBackStack` so it survives process death; each screen is a key that implements
`NavKey` and is marked `@Serializable`. A `NavDisplay` shows the top of the stack.

Each screen's state lives in an AndroidX `ViewModel`, scoped to its back-stack entry by
`rememberViewModelStoreNavEntryDecorator` and exposed to Compose as a `StateFlow` collected with
`collectAsStateWithLifecycle`. View models hold no Android framework types beyond `ViewModel`
itself.

Dependencies are wired by hand. The `Application` subclass owns one container object that builds
the database and exposes the stores; each view model is created through `viewModelFactory` with
what it needs from the container. There is no dependency-injection library.

The dependencies this adds, pinned in `gradle/libs.versions.toml`: `navigation3-runtime`,
`navigation3-ui`, `lifecycle-viewmodel-navigation3`, `lifecycle-viewmodel-compose`,
`lifecycle-runtime-compose`, `kotlinx-serialization-core`, and the Kotlin serialization compiler
plugin, which `@Serializable` keys require.

## Trade-offs

- **Navigation Compose.** The long-established library. Rejected because it is in maintenance
  mode and a new app gains nothing from starting on it.
- **No navigation library.** A hand-held list, a `when`, and a back handler would cover five
  screens with no new dependency. Rejected because scoping a view model to a screen and restoring
  the stack after process death would then be code the project writes and tests itself.
- **Hilt or another injection library.** Rejected: one database and a handful of view models do
  not justify an annotation processor and generated code. Revisit if hand wiring grows past one
  small file.

Costs accepted: Navigation 3 is a year old, so fewer worked examples exist than for its
predecessor. The serialization plugin arrives now rather than with backup, which needs it later
anyway. The combination was built, linted, and run on an Android 17 emulator, including a
minified release build, before this record was written.
