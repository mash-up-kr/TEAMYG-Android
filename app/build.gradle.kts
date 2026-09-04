import java.util.Properties

val localProperties = Properties().apply {
    rootProject.file("local.properties").inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.parfait.android.application)
    alias(libs.plugins.parfait.android.application.signing)
    alias(libs.plugins.parfait.jetpack.compose)
    alias(libs.plugins.parfait.test.unit)
    alias(libs.plugins.google.dagger.hilt)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.google.firebase)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.teamyg.parfait"

    defaultConfig {
        applicationId = "com.teamyg.parfait"
        versionCode = libs.versions.appVersionCode
            .get()
            .toInt()
        versionName = libs.versions.appVersionName.get()

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
    implementation(projects.feature.intro.api)
    implementation(projects.feature.intro.impl)

    implementation(projects.feature.app.setting.api)
    implementation(projects.feature.app.setting.impl)

    implementation(projects.feature.common.terms.api)
    implementation(projects.feature.common.terms.impl)

    implementation(projects.feature.groups.canvas.api)
    implementation(projects.feature.groups.canvas.impl)
    implementation(projects.feature.groups.enter.api)
    implementation(projects.feature.groups.enter.impl)
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
    // 매니페스트가 직접 선언한 Kakao AuthCodeHandlerActivity 의 상위 타입이 AppCompatActivity 다.
    // 컴파일 클래스패스에 없으면 lint Instantiatable 이 상속 체인을 못 풀어 릴리스 빌드가 깨진다.
    implementation(libs.androidx.appcompat)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    // AAR 로 들어오는 CameraX·DataStore 네이티브 라이브러리가 시그널로 죽으면 JVM 예외가 남지
    // 않아 기본 수집기로는 잡히지 않는다. NDK 수집기를 붙여야 그 크래시가 리포트된다.
    // nativeSymbolUploadEnabled 는 켜지 않는다. 자체 네이티브 빌드가 없어 업로드할
    // 언스트립 심볼 자체가 없고, 서드파티 .so 는 주소만 남는다.
    implementation(libs.firebase.crashlytics.ndk)
}
