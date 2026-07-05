import java.util.Properties

val localProperties = Properties().apply {
    rootProject.file("local.properties").inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.parfait.android.application)
    alias(libs.plugins.parfait.android.application.signing)
    alias(libs.plugins.parfait.jetpack.compose)
    alias(libs.plugins.google.dagger.hilt)
    alias(libs.plugins.google.ksp)
}

android {
    namespace = "com.teamyg.parfait"

    defaultConfig {
        applicationId = "com.teamyg.parfait"
        versionCode = libs.versions.versionCode
            .get()
            .toInt()
        versionName = libs.versions.versionName.get()

        manifestPlaceholders["SCHEME_KAKAO_NATIVE_APP_KEY"] =
            "kakao${localProperties.getProperty("kakao.native.app.key")}"

        buildConfigField(
            "String",
            "KAKAO_NATIVE_APP_KEY",
            "\"${localProperties.getProperty("kakao.native.app.key")}\"",
        )
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(projects.feature.login.api)
    implementation(projects.feature.login.impl)
    implementation(projects.feature.segmentation.api)
    implementation(projects.feature.segmentation.impl)
    implementation(projects.feature.camera.api)
    implementation(projects.feature.camera.impl)
    implementation(projects.feature.gallery.api)
    implementation(projects.feature.gallery.impl)
    implementation(projects.feature.splash.api)
    implementation(projects.feature.splash.impl)

    implementation(projects.feature.app.setting.api)
    implementation(projects.feature.app.setting.impl)

    implementation(projects.feature.groups.canvas.api)
    implementation(projects.feature.groups.canvas.impl)
    implementation(projects.feature.groups.enter.api)
    implementation(projects.feature.groups.enter.impl)
    implementation(projects.feature.groups.home.api)
    implementation(projects.feature.groups.home.impl)
    implementation(projects.feature.groups.list.api)
    implementation(projects.feature.groups.list.impl)
    implementation(projects.feature.groups.setting.api)
    implementation(projects.feature.groups.setting.impl)

    implementation(projects.domain)
    implementation(projects.data)
    implementation(projects.core.ui)
    implementation(projects.core.util.android)
    implementation(projects.core.util.jvm)
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.bundles.navigation)

    implementation(libs.kakao.sdk.user)
}
