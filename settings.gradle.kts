import java.net.URI

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
        maven { url = URI("https://devrepo.kakao.com/nexus/content/groups/public/") }
    }
}

rootProject.name = "parfait"
include(":app")
include(
    ":core:designsystem",
    ":core:ui",
    ":core:util",
    ":core:navigation",
)
include(":data")
include(":domain")
include(
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
    ":feature:splash:api",
    ":feature:splash:impl",
)
