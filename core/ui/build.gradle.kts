plugins {
    alias(libs.plugins.parfait.android.library)
    alias(libs.plugins.parfait.jetpack.compose)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.parfait.test.unit)
    alias(libs.plugins.parfait.test.android)
    alias(libs.plugins.parfait.test.compose)
}

android {
    namespace = "com.teamyg.parfait.core.ui"
}

dependencies {
    implementation(projects.core.util.android)
    implementation(projects.core.util.jvm)
    implementation(projects.domain)
}
