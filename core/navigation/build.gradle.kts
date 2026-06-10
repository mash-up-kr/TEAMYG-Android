plugins {
    alias(libs.plugins.teamyg.android.library)
    alias(libs.plugins.teamyg.dagger.hilt.compose)
}

android {
    namespace = "com.teamyg.parfait.core.navigation"
}

dependencies {
    implementation(projects.feature.camera.api)
    implementation(projects.feature.canvas.api)
    implementation(projects.feature.gallery.api)
    implementation(projects.feature.login.api)
    implementation(projects.feature.grouphome.api)
    implementation(projects.feature.segmentation.api)

    implementation(libs.bundles.navigation)
}
