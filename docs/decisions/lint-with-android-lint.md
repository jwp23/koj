# Lint With Android Lint

Decision: Static analysis is Android Lint, run with `./gradlew lint`, with warnings treated as
errors. The app module gets it from the Android Gradle Plugin; the pure Kotlin module applies
Google's `com.android.lint` plugin. Two checks are disabled in the app module:
`AndroidGradlePluginVersion` and `GradleDependency`.

Rationale: detekt has no stable release that supports Kotlin 2.3 or later; its 1.x line is built
for Kotlin 2.0 and 2.0 is still in alpha. Android Lint ships with the build plugin the project
already depends on, analyses Kotlin semantics rather than only style, includes the Compose
checks, and runs on a module with no Android code.

The two disabled checks report that a newer Gradle, plugin, or library version exists. With
warnings as errors they would fail every build on the day anything upstream is released.
Dependabot reports new versions instead.

Checked 2026-11-14. Revisit detekt when 2.0 is stable.
