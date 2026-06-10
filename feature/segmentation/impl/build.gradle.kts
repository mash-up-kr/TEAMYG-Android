plugins {
    alias(libs.plugins.teamyg.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.segmentation.impl"
}

dependencies {
    implementation(projects.feature.segmentation.api)
}
