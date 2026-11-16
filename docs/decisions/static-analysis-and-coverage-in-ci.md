# Static Analysis and Coverage in CI

Decision: CI runs Semgrep through Semgrep Cloud (`semgrep ci`) and SonarQube Cloud through the
`org.sonarqube` Gradle plugin, both skipped for Dependabot pull requests. Test coverage is
measured by Kover, whose aggregate XML report is passed to SonarQube.

Rationale: SonarQube Cloud's automatic analysis needs no build integration but cannot receive
coverage and reports fewer findings for Gradle projects, so analysis runs from CI instead. Kover
is JetBrains' own coverage tool for Kotlin and writes the JaCoCo-format XML that SonarQube reads.
Semgrep's community rules cover little Kotlin, so it runs through the cloud service, as in the
maintainer's other projects, where the fuller rule set applies. Dependabot pull requests are
skipped because they receive no repository secrets.

These are the only third-party Gradle plugins in the build.

## Coverage of the app module

Decision (2026-11-14): SonarQube's coverage measure excludes everything under
`app/src/main/kotlin`. Debug builds of the app collect coverage from device tests instead
(`enableAndroidTestCoverage`), and the pre-push hook blocks a push that changes the app module
when the line coverage in that report is below 80 percent. Joe's ruling (2026-11-16) measures the
80 percent over handwritten code only: Room's generated `_Impl.kt` files are left out, because most
of their lines are schema checks that no test of ours can reach. The HTML report still shows them.

Rationale: Kover sees only tests that run on the JVM, and storage and screens are tested on an
emulator, which CI does not have. Without the exclusion SonarQube would count all of that code
as untested and fail every pull request; with only the exclusion nothing would measure it. Logic
that can live in the pure Kotlin module does, where Kover and the quality gate apply. The floor
stays at 80 percent. Generated code is skipped because on a real report it measured 77 percent
when counted, while handwritten code alone measured 98 percent (73 of 74 lines).

`./gradlew createDebugAndroidTestCoverageReport` runs the device tests and writes
`app/build/reports/coverage/androidTest/debug/connected/report.xml` in JaCoCo's format. When
device tests run in CI, pass that report to SonarQube beside Kover's and remove the exclusion.

Status: the exclusion in `build.gradle.kts` covers `app/src/main/kotlin`.
