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
