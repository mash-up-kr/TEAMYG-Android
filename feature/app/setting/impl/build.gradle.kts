import java.util.Properties

// 주소가 바뀔 수 있어 저장소에 박지 않는다. -PYG_OSS_LICENSE_URL 또는 local.properties 로 넣고,
// 둘 다 없으면 빈 문자열이 되어 설정 화면이 오픈소스 라이선스 항목을 숨긴다.
val localProperties = Properties().apply {
    rootProject
        .file("local.properties")
        .takeIf { it.exists() }
        ?.inputStream()
        ?.use { load(it) }
}
val ossLicenseUrl: String = providers.gradleProperty("YG_OSS_LICENSE_URL").orNull
    ?: localProperties.getProperty("YG_OSS_LICENSE_URL").orEmpty()

plugins {
    alias(libs.plugins.parfait.module.feature.impl)
    alias(libs.plugins.parfait.test.unit)
}

android {
    namespace = "com.teamyg.parfait.feature.app.setting.impl"

    defaultConfig {
        buildConfigField("String", "OSS_LICENSE_URL", "\"$ossLicenseUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.feature.app.setting.api)
    implementation(projects.feature.common.terms.api)
    implementation(projects.feature.login.api)
}
