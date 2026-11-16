plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.sonarqube)
}

// Lock only the classpaths that compile, test, or ship project code. The build tooling's own
// classpaths (lint, test platform, plugins) are pinned by the AGP and plugin versions instead.
val lockedClasspath = Regex(".*(Compile|Runtime)Classpath", RegexOption.IGNORE_CASE)

allprojects {
    dependencyLocking { lockMode = LockMode.STRICT }
    configurations
        .matching { lockedClasspath.matches(it.name) }
        .configureEach { resolutionStrategy.activateDependencyLocking() }
}

dependencies {
    kover(project(":app"))
    kover(project(":core"))
}

sonar {
    properties {
        property("sonar.projectKey", "jwp23_koj")
        property("sonar.organization", "jwp23")
        // Fail the CI job when the quality gate fails, rather than only reporting it.
        property("sonar.qualitygate.wait", "true")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            layout.buildDirectory.file("reports/kover/report.xml").get().asFile.path,
        )
        // The app module is tested on a device, which Kover does not see, so its coverage is
        // checked by the pre-push hook instead. See
        // docs/decisions/static-analysis-and-coverage-in-ci.md.
        property("sonar.coverage.exclusions", "app/src/main/kotlin/**")
        property("sonar.issue.ignore.multicriteria", "e1")
        // kotlin:S6474 (no verification-metadata.xml) is deliberately not configured here: it is
        // a project-level issue with no file, which this file-path based setting cannot match. The
        // decision is in docs/decisions/gradle-dependency-verification-not-used.md and the issue is
        // accepted in the SonarCloud project.
        // Ignores text:S8569 on the root build file (lock file not found beside it). False
        // positive: strict locking is on and the lockfiles are committed per module, while the
        // rule only looks beside the root build file, where Gradle generates none. See
        // docs/decisions/dependency-audit-with-osv-scanner.md.
        property("sonar.issue.ignore.multicriteria.e1.ruleKey", "text:S8569")
        property("sonar.issue.ignore.multicriteria.e1.resourceKey", "build.gradle.kts")
    }
}

// The sonar plugin orders its analysis after test and compile tasks but not after Kover,
// so without this the analysis can read the coverage report before it exists.
tasks.named("sonar") { dependsOn(tasks.named("koverXmlReport")) }

// The build runs on a JDK the developer has installed; Gradle never downloads one.
tasks.updateDaemonJvm { toolchainDownloadUrls.empty() }
