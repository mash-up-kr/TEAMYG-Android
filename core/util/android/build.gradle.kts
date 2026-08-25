plugins {
    alias(libs.plugins.parfait.android.library)
    alias(libs.plugins.parfait.jetpack.compose)
    alias(libs.plugins.parfait.test.unit)
    alias(libs.plugins.parfait.test.android)
}

android {
    namespace = "com.teamyg.parfait.core.util.android"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // `:app` 의 versionName 과 같은 카탈로그 항목을 읽는다 — 사연은 AppInfo.kt 에 있다
        buildConfigField("String", "APP_VERSION_NAME", "\"${libs.versions.appVersionName.get()}\"")
    }
}

dependencies {
    implementation(projects.core.util.jvm)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
}
