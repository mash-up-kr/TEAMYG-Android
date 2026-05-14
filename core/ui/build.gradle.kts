plugins {
    alias(libs.plugins.teamyg.android.library)
    alias(libs.plugins.teamyg.jetpack.compose)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.teamyg.core.ui"
}

dependencies {
    implementation(projects.core.util)
    implementation(projects.core.designsystem)
}
