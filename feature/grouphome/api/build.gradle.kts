plugins {
    alias(libs.plugins.teamyg.module.feature.api)
}

android {
    namespace = "com.teamyg.grouphome.api"
}

dependencies {
    implementation(libs.bundles.navigation)
}
