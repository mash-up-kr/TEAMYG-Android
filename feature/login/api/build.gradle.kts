plugins {
    alias(libs.plugins.teamyg.module.feature.api)
}

android {
    namespace = "com.teamyg.login.api"
}

dependencies {
    implementation(libs.bundles.navigation)
}
