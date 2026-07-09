pluginManagement {
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
        google()
        mavenCentral()
    }
}

rootProject.name = "mangaku-extensions"

// Contrato de API compilado (lo provee la app anfitriona en runtime) y plantillas multisrc que se
// bundlean en cada APK que las usa.
include(
    ":core:tachiyomi-api",
    ":lib-multisrc:madara",
    ":lib-multisrc:mangabox",
    ":extensions:weebcentral",
    ":extensions:senmanga",
    ":extensions:comick",
    ":extensions:mangaread",
    ":extensions:manganato",
    ":extensions:mangakakalot",
    ":extensions:inmanga",
)
