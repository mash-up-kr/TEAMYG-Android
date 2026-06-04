plugins {
    alias(libs.plugins.teamyg.android.library)
    alias(libs.plugins.teamyg.jetpack.compose)
    alias(libs.plugins.teamyg.dagger.hilt.compose)
}

android {
    namespace = "com.teamyg.core.ui"
}

dependencies {
    implementation(projects.core.analytics)
    implementation(projects.core.designsystem)
    implementation(projects.core.util)
}
