plugins {
    alias(libs.plugins.parfait.android.application)
    alias(libs.plugins.parfait.jetpack.compose)
    alias(libs.plugins.google.dagger.hilt)
    alias(libs.plugins.google.ksp)
}

android {
    namespace = "com.teamyg.parfait.preview"

    defaultConfig {
        applicationId = "com.teamyg.parfait.preview"
        versionCode = libs.versions.previewVersionCode
            .get()
            .toInt()
        versionName = libs.versions.previewVersionName.get()
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.util.android)
    implementation(projects.core.util.jvm)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.bundles.navigation)
}
