package com.teamyg.parfait.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.teamyg.parfait.buildlogic.utils.extensions.androidTestImplementation
import com.teamyg.parfait.buildlogic.utils.extensions.debugImplementation
import com.teamyg.parfait.buildlogic.utils.extensions.libs
import com.teamyg.parfait.buildlogic.utils.extensions.project
import com.teamyg.parfait.buildlogic.utils.extensions.testImplementation
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

internal fun Project.setConfigTestUnit() {
    dependencies {
        testImplementation(libs.bundles.test.unit)
        testImplementation(project(":core:testing"))
    }

    // 루트 `test` 태스크는 Android 라이브러리 모듈에서 testDebugUnitTest 와
    // testReleaseUnitTest 를 둘 다 돌려 같은 테스트를 두 번 실행한다. release 변형의
    // 유닛 테스트 컴포넌트를 꺼서 중복 실행을 막는다.
    // AGP 9.2.1 기준: VariantBuilder.enableUnitTest(구 네이밍)는 여전히 존재하지만,
    // LibraryVariantBuilder 가 구현하는 HasHostTestsBuilder 의 새 host-test 네이밍인
    // `hostTests[HostTestBuilder.UNIT_TEST_TYPE]` 을 대신 쓴다.
    extensions.findByType(LibraryAndroidComponentsExtension::class)?.apply {
        beforeVariants(selector().withBuildType("release")) { variantBuilder ->
            variantBuilder.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = false
        }
    }
}

internal fun Project.setConfigTestAndroid() {
    dependencies {
        androidTestImplementation(libs.bundles.test.android)
        androidTestImplementation(project(":core:testing"))
    }

    val applicationExtension: ApplicationExtension? =
        extensions.findByType(ApplicationExtension::class)
    val libraryExtension: LibraryExtension? =
        extensions.findByType(LibraryExtension::class)

    when {
        applicationExtension != null -> applicationExtension.configureInstrumentationTest()
        libraryExtension != null -> libraryExtension.configureInstrumentationTest()
        else -> error("must be applied com.android.application or com.android.library")
    }
}

private fun ApplicationExtension.configureInstrumentationTest() {
    defaultConfig.testInstrumentationRunner = ANDROID_JUNIT_RUNNER
    testOptions.animationsDisabled = true
}

private fun LibraryExtension.configureInstrumentationTest() {
    defaultConfig.testInstrumentationRunner = ANDROID_JUNIT_RUNNER
    testOptions.animationsDisabled = true
}

private const val ANDROID_JUNIT_RUNNER = "androidx.test.runner.AndroidJUnitRunner"

internal fun Project.setConfigTestCompose() {
    dependencies {
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.compose.ui.test.junit4)

        debugImplementation(libs.androidx.compose.ui.test.manifest)
    }
}
