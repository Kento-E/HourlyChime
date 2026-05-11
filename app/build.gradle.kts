import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val localProps = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun Properties.nonBlank(name: String): String? = getProperty(name)?.takeIf { it.isNotBlank() }

val releaseStoreFilePath = localProps.nonBlank("RELEASE_STORE_FILE")
val releaseStorePassword = localProps.nonBlank("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = localProps.nonBlank("RELEASE_KEY_ALIAS")
val releaseKeyPassword = localProps.nonBlank("RELEASE_KEY_PASSWORD")
val hasReleaseSigningConfig =
    releaseStoreFilePath != null &&
        releaseStorePassword != null &&
        releaseKeyAlias != null &&
        releaseKeyPassword != null

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
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    dependsOn("verifyReleaseSigning", "assembleRelease")

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

tasks.register("verifyReleaseSigning") {
    group = "verification"
    description = "Verify release signing properties before assembling or distributing release build."

    doLast {
        val missingKeys = buildList {
            if (releaseStoreFilePath == null) add("RELEASE_STORE_FILE")
            if (releaseStorePassword == null) add("RELEASE_STORE_PASSWORD")
            if (releaseKeyAlias == null) add("RELEASE_KEY_ALIAS")
            if (releaseKeyPassword == null) add("RELEASE_KEY_PASSWORD")
        }

        if (missingKeys.isNotEmpty()) {
            throw GradleException(
                "Missing release signing values in local.properties: ${missingKeys.joinToString(", ")}" +
                    ". Unit tests can run without these values, but release build/distribution requires them."
            )
        }

        val keystorePath = releaseStoreFilePath!!
        val keystoreFile = file(keystorePath)
        if (!keystoreFile.exists()) {
            throw GradleException("Release keystore not found: ${keystoreFile.absolutePath}")
        }
    }
}