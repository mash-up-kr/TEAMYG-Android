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
    implementation(projects.feature.grouphome.api)
    implementation(projects.feature.grouphome.impl)
    implementation(projects.feature.login.api)
    implementation(projects.feature.login.impl)

    implementation(projects.domain)
    implementation(projects.core.ui)
    implementation(projects.core.util)
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.bundles.navigation)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
