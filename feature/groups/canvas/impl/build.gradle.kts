import java.util.Properties

val localProperties = Properties().apply {
    rootProject.file("local.properties").inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.parfait.module.feature.impl)
    alias(libs.plugins.parfait.test.unit)
    alias(libs.plugins.parfait.test.android)
    alias(libs.plugins.parfait.test.compose)
}

android {
    namespace = "com.teamyg.parfait.feature.groups.canvas.impl"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // 인스타그램 스토리 공유 귀속에 쓰는 Facebook App ID. 공개 식별자지만 레포에 남기지 않으려고
        // local.properties(facebook.app.id)에서 읽는다 — 없으면 빈 값이라 귀속만 빠지고 공유는 동작한다
        buildConfigField(
            "String",
            "FACEBOOK_APP_ID",
            "\"${localProperties.getProperty("facebook.app.id").orEmpty()}\"",
        )
    }
}

dependencies {
    implementation(projects.feature.groups.canvas.api)
    implementation(projects.feature.camera.api)
    implementation(projects.feature.gallery.api)
    implementation(projects.feature.groups.setting.api)
    implementation(projects.core.util.android)
}
