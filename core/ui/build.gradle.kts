plugins {
    alias(libs.plugins.teamyg.android.library)
    alias(libs.plugins.teamyg.jetpack.compose)
    alias(libs.plugins.google.ksp)
}

android {
    namespace = "com.teamyg.parfait.core.ui"
}

dependencies {
    implementation(projects.core.util)
    implementation(projects.core.designsystem)
}
