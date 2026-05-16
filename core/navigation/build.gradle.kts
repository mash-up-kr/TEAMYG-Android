plugins {
    alias(libs.plugins.teamyg.android.library)
    alias(libs.plugins.teamyg.dagger.hilt.compose)
}

android {
    namespace = "com.teamyg.navigation"
}

dependencies {
    implementation(projects.feature.login.api)
    implementation(projects.feature.grouphome.api)

    implementation(libs.bundles.navigation)
}
