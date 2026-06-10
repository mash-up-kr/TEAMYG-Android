plugins {
    alias(libs.plugins.parfait.android.library)
    alias(libs.plugins.parfait.jetpack.compose)
    alias(libs.plugins.google.ksp)
}

android {
    namespace = "com.teamyg.parfait.core.ui"
}

dependencies {
    implementation(projects.core.util)
    implementation(projects.core.designsystem)
}
