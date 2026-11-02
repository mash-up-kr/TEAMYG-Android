plugins {
    alias(libs.plugins.parfait.android.library)
    alias(libs.plugins.parfait.jetpack.compose)
    alias(libs.plugins.parfait.test.android)
    alias(libs.plugins.parfait.test.compose)
}

android {
    namespace = "com.teamyg.parfait.core.designsystem"
}

dependencies {
    implementation(projects.core.util.android)
    implementation(projects.core.util.jvm)

    implementation(libs.lottie.compose)
}
