plugins {
    alias(libs.plugins.teamyg.android.application)
    alias(libs.plugins.teamyg.jetpack.compose)
    alias(libs.plugins.teamyg.dagger.hilt)
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
    implementation(projects.core.ui)
    implementation(projects.core.util)
    implementation(projects.core.designsystem)

    implementation(projects.domain)

    implementation(projects.feature.sample)
    implementation(projects.feature.segmentation)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
