plugins {
    alias(libs.plugins.teamyg.android.library)
    alias(libs.plugins.teamyg.jetpack.compose)
}

android {
    namespace = "com.teamyg.parfait.core.designsystem"
}

dependencies {
    implementation(projects.core.util)
}
