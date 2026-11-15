# Build Toolchain Versions

Decision: Version pins for Kotlin, Gradle, and the Android Gradle Plugin come from the
compatibility table Kotlin publishes, never from sample projects. Within what the table lists as
fully supported for the Kotlin version in use, the project takes the latest stable release of
each. The build runs on the newest JDK that the pinned Gradle version supports and that is still
maintained. The versions themselves live in `gradle/libs.versions.toml`, the Gradle wrapper, and
`gradle/gradle-daemon-jvm.properties`.

Rationale: Kotlin, Gradle, and the Android Gradle Plugin each release on their own schedule, and
the newest of each are regularly outside the range the others were tested with. Kotlin's
documentation says newer Gradle and plugin versions may work but "you might encounter
deprecation warnings or some new features might not work". Staying inside the table trades the
newest features for a build with no unexplained warnings.

As of 2026-11-14 that gave Kotlin 2.4.21, Android Gradle Plugin 9.3.1, Gradle 9.7.0, and JDK 25.
Gradle 9.7.0 supports JVM 17 to 26. JDK 26 is a short-term release that JDK 27 has replaced,
so 25, a long-term support release, is the newest one worth pinning.

The table: https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin

Dependabot does not consult the table, so `.github/dependabot.yml` tells it to ignore the Gradle
wrapper and the Android Gradle Plugin. A Dependabot PR for Kotlin is the trigger to read the
table and bump Kotlin, Gradle, and the Android Gradle Plugin together by hand.

## Guard against pairings outside the table

`gradle.properties` sets `org.gradle.warning.mode=fail`, so any Gradle warning fails the build.
Measured on 2026-11-14 against Kotlin 2.4.21, whose table row allows Gradle up to 9.7.0 and the
Android Gradle Plugin up to 9.3.1:

- Gradle 9.8.2 with AGP 9.3.1 logs a Gradle deprecation warning at configuration
  (`Configuration.setVisible(boolean)`). With the setting the build fails; without it the build
  passes.
- Gradle 9.7.0 with AGP 9.5.1 logs no Gradle warning, so the setting does not catch it. The
  `lint` task fails instead: the `androidx.lifecycle` lint checks reference Kotlin APIs the newer
  lint does not have (`ObsoleteLintCustomCheck`). That failure comes from the lint gate that
  already runs in CI and the pre-commit hook, not from this setting.
- The pinned versions build with no warnings, so the setting does not fail the current build.

Limits:

- A reused configuration cache entry does not replay configuration-time warnings. A build that
  failed because of a warning stores an entry, and the next build reuses it and passes. The
  pre-commit hook therefore runs Gradle with `--no-configuration-cache`, which cost about 0.25
  seconds per run on this project when measured. CI starts without an entry. A plain `./gradlew`
  run by hand can still reuse a stale entry.
- Any Gradle warning fails the build, not only toolchain ones. A plugin update that adds a
  deprecation warning fails CI until it is fixed or reverted.
