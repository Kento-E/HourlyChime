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
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        maven {
            url = uri("$rootDir/local-maven")
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "MCP Token Viewer"
include(":app")
