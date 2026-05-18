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
        if (disableDirectGoogleMaven && googleMavenMirrorUrls.isEmpty()) {
            throw GradleException(
                "disableDirectGoogleMaven=true (or DISABLE_DIRECT_GOOGLE_MAVEN=true) is set, " +
                    "but no Google Maven mirrors are configured. " +
                    "Set googleMavenMirrorUrls or GOOGLE_MAVEN_MIRROR_URLS."
            )
        }
        gradle.extra["googleMavenMirrorUrls"] = googleMavenMirrorUrls
        gradle.extra["disableDirectGoogleMaven"] = disableDirectGoogleMaven

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
        val googleMavenMirrorUrls = (gradle.extra["googleMavenMirrorUrls"] as? List<*>)
            ?.map { it as? String ?: throw GradleException("Invalid googleMavenMirrorUrls entry type.") }
            ?: throw GradleException("Missing googleMavenMirrorUrls in Gradle extra properties.")
        val disableDirectGoogleMaven = gradle.extra["disableDirectGoogleMaven"] as? Boolean
            ?: throw GradleException("Missing disableDirectGoogleMaven in Gradle extra properties.")

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
