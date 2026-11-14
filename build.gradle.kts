plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
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
    }
}

// The build runs on a JDK the developer has installed; Gradle never downloads one.
tasks.updateDaemonJvm { toolchainDownloadUrls.empty() }
