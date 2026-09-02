plugins {
    alias(libs.plugins.parfait.module.feature.impl)
    alias(libs.plugins.parfait.test.unit)
}

android {
    namespace = "com.teamyg.parfait.feature.segmentation.impl"
}

dependencies {
    implementation(projects.feature.segmentation.api)
    implementation(libs.google.mlkit.subject.segmentation)
    implementation(projects.feature.groups.canvas.api)
    implementation(projects.core.util.android)
    implementation(projects.core.util.jvm)
}
