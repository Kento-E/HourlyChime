pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application" && requested.version != null) {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
            if (requested.id.id == "com.google.gms.google-services" && requested.version != null) {
                useModule("com.google.gms:google-services:${requested.version}")
            }
            if (requested.id.id == "com.google.firebase.appdistribution" && requested.version != null) {
                useModule("com.google.firebase:firebase-appdistribution-gradle:${requested.version}")
            }
        }
    }
    repositories {
        val googleMavenMirrorUrls = (
            providers.gradleProperty("googleMavenMirrorUrls").orNull
                ?: providers.environmentVariable("GOOGLE_MAVEN_MIRROR_URLS").orNull
            )
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            .orEmpty()
        val disableDirectGoogleMaven = providers.gradleProperty("disableDirectGoogleMaven")
            .orElse(providers.environmentVariable("DISABLE_DIRECT_GOOGLE_MAVEN"))
            .map { it.equals("true", ignoreCase = true) }
            .getOrElse(false)

        googleMavenMirrorUrls.forEachIndexed { index, mirrorUrl ->
            maven {
                name = "GoogleMirror${index + 1}"
                url = uri(mirrorUrl)
                content {
                    includeGroupByRegex("com\\.android.*")
                    includeGroupByRegex("com\\.google.*")
                    includeGroupByRegex("androidx.*")
                }
            }
        }
        if (!disableDirectGoogleMaven) {
            google {
                content {
                    includeGroupByRegex("com\\.android.*")
                    includeGroupByRegex("com\\.google.*")
                    includeGroupByRegex("androidx.*")
                }
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val googleMavenMirrorUrls = (
            providers.gradleProperty("googleMavenMirrorUrls").orNull
                ?: providers.environmentVariable("GOOGLE_MAVEN_MIRROR_URLS").orNull
            )
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            .orEmpty()
        val disableDirectGoogleMaven = providers.gradleProperty("disableDirectGoogleMaven")
            .orElse(providers.environmentVariable("DISABLE_DIRECT_GOOGLE_MAVEN"))
            .map { it.equals("true", ignoreCase = true) }
            .getOrElse(false)

        mavenLocal()
        maven {
            url = uri("$rootDir/local-maven")
        }
        googleMavenMirrorUrls.forEachIndexed { index, mirrorUrl ->
            maven {
                name = "GoogleMirror${index + 1}"
                url = uri(mirrorUrl)
                content {
                    includeGroupByRegex("com\\.android.*")
                    includeGroupByRegex("com\\.google.*")
                    includeGroupByRegex("androidx.*")
                }
            }
        }
        if (!disableDirectGoogleMaven) {
            google()
        }
        mavenCentral()
    }
}

rootProject.name = "MCP Token Viewer"
include(":app")
