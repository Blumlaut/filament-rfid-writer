pluginManagement {
    repositories {
        google {
            content { includeGroupByRegex("com\\.android.*") }
            content { includeGroupByRegex("com\\.google.*") }
            content { includeGroupByRegex("androidx.*") }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content { includeGroupByRegex("com\\.android.*") }
            content { includeGroupByRegex("com\\.google.*") }
            content { includeGroupByRegex("androidx.*") }
        }
        mavenCentral()
    }
}

rootProject.name = "FilamentTagWriter"
include(":app")
