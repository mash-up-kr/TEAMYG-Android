plugins {
    alias(libs.plugins.parfait.module.feature.impl)
    alias(libs.plugins.parfait.test.unit)
}

android {
    namespace = "com.teamyg.parfait.feature.gallery.impl"
}

dependencies {
    implementation(projects.feature.gallery.api)
    implementation(projects.feature.camera.api)
    implementation(projects.feature.segmentation.api)
}
