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
