enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "teamyg"
include(":app")
include(
    ":core:analytics",
    ":core:designsystem",
    ":core:model",
    ":core:ui",
    ":core:util",
    ":core:navigation",
)
include(":data")
include(":domain")
include(
    ":feature:sample",
    ":feature:login:api",
    ":feature:login:impl",
    ":feature:grouphome:api",
    ":feature:grouphome:impl",
    ":feature:segmentation:api",
    ":feature:segmentation:impl",
    ":feature:camera:api",
    ":feature:camera:impl",
    ":feature:gallery:api",
    ":feature:gallery:impl",
    ":feature:canvas:api",
    ":feature:canvas:impl",
)
