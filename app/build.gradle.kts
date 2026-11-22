import java.time.LocalDateTime

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

// The dist pipeline's optional single-ABI restriction — the counterpart of
// RN's reactNativeArchitectures (`pn apk:arm64-v8a`): one ABI keeps only
// that architecture's native libraries (arm64-v8a on modern phones); unset
// builds the universal APK. The same value is stamped onto the dist name.
//
//   ./gradlew :app:distReleaseApk                       → universal
//   ./gradlew :app:distReleaseApk -PfeedahAbi=arm64-v8a → single-ABI
val feedahAbi = (project.findProperty("feedahAbi") as String?)?.takeIf { it.isNotEmpty() }

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

        // A local binding: the property smart-casts inside the block.
        val distAbi = feedahAbi
        if (distAbi != null) {
            ndk { abiFilters.add(distAbi) }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            // The dist APK must install: a personal app signs its release
            // with the debug keystore (the same choice RN's android project
            // makes — a proper keystore waits for a store release).
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            // Robolectric renders real resources and compose trees for the
            // layout-screenshot tests.
            isIncludeAndroidResources = true
        }
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

// The dist pipeline, the counterpart of RN's scripts/build-apk.mjs
// (`pn apk:arm64-v8a`): assemble the release APK, copy it to <root>/dist/
// (gitignored) under a stamped name:
//
//   feedah-<yyMMdd-HHmm>-<short commit>[-<abi>].apk
//   e.g. feedah-261120-2134-0a3c-arm64-v8a.apk
//
// The RN script exists because a pnpm script is the only build entry point
// there; the AN build system owns this natively, so the wrapper is a task.
// The ABI comes in through -PfeedahAbi (the comment at the top of the
// file); the dataset through -PfeedahDataset.
tasks.register("distReleaseApk") {
    group = "feedah"
    description =
        "Assembles the release APK into dist/ under the stamped name (see the comment above this task)."

    // Everything Gradle is resolved at configuration time — doLast only
    // closes over plain values, which is what the configuration cache
    // requires of a task action.
    val apkDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
    val distDir = rootProject.layout.projectDirectory.dir("dist").asFile
    val abiSuffix = feedahAbi?.let { "-$it" } ?: ""
    // The commit does not move while a build runs, so configuration time is
    // as exact as execution time.
    val shortCommit = providers.exec {
        commandLine("git", "rev-parse", "--short=4", "HEAD")
        workingDir(rootDir)
    }.standardOutput.asText.get().trim()

    dependsOn("assembleRelease")
    doLast {
        val apks = apkDir.listFiles { f -> f.isFile && f.name.endsWith(".apk") }
            ?.sortedBy { it.name }
            ?: emptyList()
        val apk = apks.firstOrNull()
            ?: throw GradleException(
                "No APK in app/build/outputs/apk/release — did assembleRelease succeed?",
            )

        // The timestamp is taken when the APK is finished (inside doLast),
        // not when the build was configured, so it reflects when the
        // artifact was actually produced — the same rule the RN script
        // states.
        val now = LocalDateTime.now()
        val stamp = "%02d%02d%02d-%02d%02d".format(
            now.year % 100,
            now.monthValue,
            now.dayOfMonth,
            now.hour,
            now.minute,
        )
        val name = "feedah-$stamp-$shortCommit$abiSuffix.apk"

        distDir.mkdirs()
        apk.copyTo(distDir.resolve(name), overwrite = true)
        println("APK ready: dist/$name")
    }
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
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}