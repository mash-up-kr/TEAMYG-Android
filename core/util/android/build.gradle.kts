plugins {
    alias(libs.plugins.parfait.android.library)
    alias(libs.plugins.parfait.jetpack.compose)
    alias(libs.plugins.parfait.test.unit)
    alias(libs.plugins.parfait.test.android)
}

android {
    namespace = "com.teamyg.parfait.core.util.android"
}

dependencies {
    implementation(projects.core.util.jvm)

    implementation(libs.androidx.core.ktx)
}
