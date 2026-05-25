plugins {
    alias(libs.plugins.teamyg.module.feature.impl)
}

android {
    namespace = "com.teamyg.segmentation.impl"
}

dependencies {
    implementation(projects.feature.segmentation.api)
    implementation(projects.feature.camera.api)
    implementation(projects.feature.gallery.api)
}
