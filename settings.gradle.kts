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
include(
    ":app",
    ":app-preview",
)
include(
    ":core:designsystem",
    ":core:ui",
    ":core:util:android",
    ":core:util:jvm",
    ":core:navigation",
)
include(":data")
include(":domain")
include(
    ":feature:login:api",
    ":feature:login:impl",
    ":feature:segmentation:api",
    ":feature:segmentation:impl",
    ":feature:camera:api",
    ":feature:camera:impl",
    ":feature:gallery:api",
    ":feature:gallery:impl",
    ":feature:intro:api",
    ":feature:intro:impl",
)

include(
    ":feature:app:setting:api",
    ":feature:app:setting:impl",
)

include(
    ":feature:common:terms:api",
    ":feature:common:terms:impl",
)

include(
    ":feature:groups:canvas:api",
    ":feature:groups:canvas:impl",
    ":feature:groups:enter:api",
    ":feature:groups:enter:impl",
    ":feature:groups:home:api",
    ":feature:groups:home:impl",
    ":feature:groups:list:api",
    ":feature:groups:list:impl",
    ":feature:groups:setting:api",
    ":feature:groups:setting:impl",
)
