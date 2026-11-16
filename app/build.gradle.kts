plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kover)
}

room3 { schemaDirectory("$projectDir/schemas") }

android {
    namespace = "io.github.jwp23.koj"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.jwp23.koj"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = providers.gradleProperty("koj.versionCode").map { it.toInt() }.getOrElse(1)
        versionName = providers.gradleProperty("koj.versionName").getOrElse("0.1.0")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The release key exists only in CI secrets and an offline copy. A build without these
    // variables produces an unsigned release APK, which cannot be installed.
    val releaseKeystore = providers.environmentVariable("KOJ_KEYSTORE_FILE")
    if (releaseKeystore.isPresent) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystore.get())
                storePassword = providers.environmentVariable("KOJ_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("KOJ_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("KOJ_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        debug { enableAndroidTestCoverage = true }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    buildFeatures { compose = true }

    lint {
        warningsAsErrors = true
        abortOnError = true
        // These two only report that a newer version exists. With warnings as errors they would
        // fail the build on every upstream release; Dependabot reports new versions instead.
        disable += listOf("AndroidGradlePluginVersion", "GradleDependency")
    }

    // Stripping native libraries needs the NDK, which the project does not otherwise use.
    packaging { jniLibs { keepDebugSymbols += "**/*.so" } }
}

kotlin { jvmToolchain(libs.versions.jdk.get().toInt()) }

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.room3.runtime)
    // Declared so the app ships the coroutines version the test library is built for.
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.androidx.room3.compiler)

    // Registers the empty activity that createComposeRule() hosts a composable in.
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Declared because the older version Compose's test library brings in crashes on Android 17.
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
