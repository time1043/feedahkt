plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Which word data the app is built with: `mock` (the committed samples, in
// src/mock/assets) or `real` (a local-only directory, src/real/assets). This
// is the single place the choice is made — the code only ever reads
// assets/data/<bucket>.json.
//
//   ./gradlew :app:assembleDebug                      → mock
//   ./gradlew :app:assembleDebug -PfeedahDataset=real → real
//
// Unresolved on a machine without src/real/assets, which is why `mock` is
// the default: the repo always builds.
val feedahDataset = (project.findProperty("feedahDataset") as String?)?.takeIf { it.isNotEmpty() } ?: "mock"

android {
    namespace = "com.time1043.feedah_kt"
    compileSdk {
        version = release(37)
    }

    sourceSets {
        getByName("main") {
            assets.srcDir("src/$feedahDataset/assets")
        }
        // The instrumented tests always walk the tracked mock buckets,
        // whatever dataset this machine builds the app with: they seed from
        // the TEST APK's assets (the instrumentation's context), so a
        // feedahDataset=real default never stretches a full-suite run into
        // per-card transactions over the real vocabulary.
        getByName("androidTest") {
            assets.srcDir("src/mock/assets")
        }
    }

    defaultConfig {
        applicationId = "com.time1043.feedah_kt"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// The Java compile target is 11 (compileOptions above); the classic Kotlin
// plugin must agree or KSP's task fails its target validation.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

// Room's schema snapshots export here — the counterpart of drizzle-kit's
// generated `drizzle/` directory, committed so migrations are reviewable
// and AutoMigrations can diff one version against the next.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.sqlite.framework)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}