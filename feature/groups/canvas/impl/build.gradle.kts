plugins {
    alias(libs.plugins.parfait.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.groups.canvas.impl"
}

dependencies {
    implementation(projects.feature.groups.canvas.api)
    implementation(projects.feature.camera.api)
    implementation(projects.feature.gallery.api)
    implementation(projects.feature.segmentation.api)
    implementation(projects.core.util.android)
}
