plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kover)
}

kotlin { jvmToolchain(libs.versions.jdk.get().toInt()) }

lint {
    warningsAsErrors = true
    abortOnError = true
}

dependencies { testImplementation(libs.junit) }
