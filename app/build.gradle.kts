plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// Monotonic version code = number of commits, so every CI build is a valid in-place update of the previous one (CI uses fetch-depth: 0).
val gitCommitCount: Int = runCatching {
    ProcessBuilder("git", "rev-list", "--count", "HEAD").directory(rootDir).redirectErrorStream(true).start()
        .inputStream.bufferedReader().readText().trim().toInt()
}.getOrDefault(1)

android {
    namespace = "com.ohadsam.specialevents"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ohadsam.specialevents"
        minSdk = 26
        targetSdk = 35
        versionCode = 100 + gitCommitCount
        versionName = "0.7.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // One fixed key signs debug AND release builds: Android only updates an installed app in place when the signature matches,
    // so users never have to uninstall. It is committed on purpose and is NOT a store key (see AGENT.md); never change it once
    // users installed the app. For a store release, supply KEYSTORE_* secrets (release.yml) — users then reinstall once.
    signingConfigs {
        create("sideload") {
            storeFile = rootProject.file("app/sideload.jks")
            storePassword = "specialevents-sideload"
            keyAlias = "specialevents"
            keyPassword = "specialevents-sideload"
        }
        create("release") {
            val ks = System.getenv("KEYSTORE_FILE")
            if (ks != null) {
                storeFile = file(ks)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("sideload")
        }
        release {
            signingConfig = if (System.getenv("KEYSTORE_FILE") != null) signingConfigs.getByName("release") else signingConfigs.getByName("sideload")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

// Room writes its schema JSON here; commit it, it is the base for migration tests.
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

// Lint is advisory until the code base settles (see docs/PLAN.md); flip to false to enforce.
ktlint { ignoreFailures.set(true) }
detekt {
    ignoreFailures = true
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt.yml"))
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coroutines.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.coroutines.test)
}
