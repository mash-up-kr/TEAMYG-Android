plugins {
    alias(libs.plugins.parfait.module.feature.impl)
    alias(libs.plugins.parfait.test.unit)
}

android {
    namespace = "com.teamyg.parfait.feature.camera.impl"
}

dependencies {
    implementation(projects.feature.camera.api)
    implementation(projects.feature.segmentation.api)
    implementation(projects.feature.groups.canvas.api)

    implementation(libs.bundles.camerax)
}
