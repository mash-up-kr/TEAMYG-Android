import java.util.Properties

// 서명 비밀값(YG_RELEASE_*)과 같은 방식으로 다룬다 — CI 는 -P/환경변수(findProperty), 로컬은
// local.properties 에서 읽는다. 커밋된 소스엔 값이 남지 않고, 키 템플릿은 local.default.properties 에 있다
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val facebookAppId = (
    findProperty(
        "YG_FACEBOOK_APP_ID",
    )?.toString() ?: localProperties.getProperty("YG_FACEBOOK_APP_ID")
    ).orEmpty()

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
        // 인스타그램 스토리 공유 귀속용 Facebook App ID. 없으면 빈 값 — 귀속만 빠지고 공유는 동작한다
        buildConfigField("String", "FACEBOOK_APP_ID", "\"$facebookAppId\"")
    }
}

dependencies {
    implementation(projects.feature.groups.canvas.api)
    implementation(projects.feature.camera.api)
    implementation(projects.feature.gallery.api)
    implementation(projects.feature.groups.setting.api)
    implementation(projects.core.util.android)
}
