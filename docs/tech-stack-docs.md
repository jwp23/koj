# Tech Stack Documentation

Where to find the reference documentation for what koj is built with. Read the relevant page
before working against a library's API; do not rely on memory for signatures or options.

Versions live only in `gradle/libs.versions.toml`.

## Language and Build

| Tool | Documentation |
|------|---------------|
| Kotlin | https://kotlinlang.org/docs/home.html |
| Kotlin standard library | https://kotlinlang.org/api/core/kotlin-stdlib/ |
| Gradle | https://docs.gradle.org/9.7.0/userguide/userguide.html |
| Gradle version catalogs | https://docs.gradle.org/9.7.0/userguide/version_catalogs.html |
| Gradle dependency locking | https://docs.gradle.org/9.7.0/userguide/dependency_locking.html |
| Android Gradle Plugin | https://developer.android.com/build |
| Android Gradle Plugin DSL | https://developer.android.com/reference/tools/gradle-api |

The Gradle links name a version. Change them when the wrapper version changes.

Which Gradle and Android Gradle Plugin versions a Kotlin version supports:
https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin

## Android Libraries

| Library | Documentation |
|---------|---------------|
| Jetpack Compose | https://developer.android.com/develop/ui/compose/documentation |
| Room | https://developer.android.com/training/data-storage/room |
| AlarmManager | https://developer.android.com/develop/background-work/services/alarms |
| WorkManager | https://developer.android.com/develop/background-work/background-tasks/persistent |
| Notifications | https://developer.android.com/develop/ui/views/notifications |
| System file picker | https://developer.android.com/training/data-storage/shared/documents-files |
| kotlinx.serialization | https://kotlinlang.org/docs/serialization.html |

## Testing

| Tool | Documentation |
|------|---------------|
| JUnit 4 | https://junit.org/junit4/ |
| Android testing | https://developer.android.com/training/testing |
| Compose UI tests | https://developer.android.com/develop/ui/compose/testing |

## Checks

| Tool | Documentation |
|------|---------------|
| ktfmt | https://github.com/Kotlin/ktfmt |
| Android Lint | https://developer.android.com/studio/write/lint |
| osv-scanner | https://google.github.io/osv-scanner/ |
| betterleaks | https://github.com/betterleaks/betterleaks |
| Kover | https://github.com/Kotlin/kotlinx-kover |
| SonarScanner for Gradle | https://docs.sonarsource.com/sonarqube-cloud/advanced-setup/ci-based-analysis/sonarscanner-for-gradle |

## Devices

| Tool | Documentation |
|------|---------------|
| adb | https://developer.android.com/tools/adb |
| Emulator command line | https://developer.android.com/studio/run/emulator-commandline |
