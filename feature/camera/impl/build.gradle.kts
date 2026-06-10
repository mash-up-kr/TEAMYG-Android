plugins {
    alias(libs.plugins.teamyg.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.camera.impl"
}

dependencies {
    implementation(projects.feature.camera.api)

    implementation(libs.bundles.camerax)
}
