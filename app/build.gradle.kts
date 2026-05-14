plugins {
    alias(libs.plugins.teamyg.android.application)
    alias(libs.plugins.teamyg.jetpack.compose)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.teamyg"

    defaultConfig {
        applicationId = "com.teamyg"
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(projects.feature.sample)
    implementation(projects.domain)
    implementation(projects.core.ui)
    implementation(projects.core.util)
    implementation(projects.core.designsystem)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.bundles.navigation)
}
