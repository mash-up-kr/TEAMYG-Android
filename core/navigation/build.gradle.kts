plugins {
    alias(libs.plugins.parfait.android.library)
    alias(libs.plugins.parfait.dagger.hilt.compose)
}

android {
    namespace = "com.teamyg.parfait.core.navigation"
}

dependencies {
    implementation(projects.feature.camera.api)
    implementation(projects.feature.gallery.api)
    implementation(projects.feature.login.api)
    implementation(projects.feature.segmentation.api)
    implementation(projects.feature.splash.api)

    implementation(projects.feature.app.setting.api)

    implementation(projects.feature.groups.canvas.api)
    implementation(projects.feature.groups.enter.api)
    implementation(projects.feature.groups.home.api)
    implementation(projects.feature.groups.list.api)
    implementation(projects.feature.groups.setting.api)

    implementation(libs.bundles.navigation)
}
