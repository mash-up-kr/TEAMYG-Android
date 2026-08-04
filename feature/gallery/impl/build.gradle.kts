plugins {
    alias(libs.plugins.parfait.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.gallery.impl"
}

dependencies {
    implementation(projects.feature.gallery.api)
    implementation(projects.feature.camera.api)
}
