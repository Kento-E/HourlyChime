import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val localProps = Properties().apply {
    rootProject.file("local.properties").inputStream().use { load(it) }
}

val firebaseAppId = "1:1052281628451:android:3d20d418e11e45e48f0e96"
val firebaseTesters = "esashika.kento@icloud.com"
val firebaseReleaseNotes = "FCM token viewer build"
val firebaseCommand = if (System.getProperty("os.name").startsWith("Windows")) {
    "firebase.cmd"
} else {
    "firebase"
}

android {
    namespace = "com.example.hourlychime"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.hourlychime"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProps["RELEASE_STORE_FILE"] as String)
            storePassword = localProps["RELEASE_STORE_PASSWORD"] as String
            keyAlias = localProps["RELEASE_KEY_ALIAS"] as String
            keyPassword = localProps["RELEASE_KEY_PASSWORD"] as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register<Exec>("appDistributionUploadRelease") {
    group = "distribution"
    description = "Upload the release APK to Firebase App Distribution via Firebase CLI."
    dependsOn("assembleRelease")

    doFirst {
        val apkFile = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        if (!apkFile.exists()) {
            throw GradleException("Release APK not found: ${apkFile.absolutePath}")
        }

        commandLine(
            firebaseCommand,
            "appdistribution:distribute",
            apkFile.absolutePath,
            "--app",
            firebaseAppId,
            "--testers",
            firebaseTesters,
            "--release-notes",
            firebaseReleaseNotes,
        )
    }
}