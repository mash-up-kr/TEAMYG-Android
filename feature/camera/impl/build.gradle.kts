plugins {
    alias(libs.plugins.teamyg.module.feature.impl)
}

android {
    namespace = "com.teamyg.camera.impl"
}

dependencies {
    implementation(projects.feature.camera.api)

    implementation(libs.bundles.camerax)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
