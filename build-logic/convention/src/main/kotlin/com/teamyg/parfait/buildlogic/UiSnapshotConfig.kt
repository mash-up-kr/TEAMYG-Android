package com.teamyg.parfait.buildlogic

import com.android.build.api.dsl.LibraryExtension
import com.github.takahirom.roborazzi.AnnotationFilter
import com.teamyg.parfait.buildlogic.utils.extensions.libs
import com.teamyg.parfait.buildlogic.utils.extensions.testImplementation
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

internal fun Project.setConfigUiSnapshot() {
    with(plugins) {
        apply(libs.plugins.roborazzi.get().pluginId)
    }

    val libraryExtension = extensions.findByType(LibraryExtension::class)
        ?: error("must be applied com.android.library")

    val isRoborazziRun = isRoborazziRun()

    extensions.configure<RoborazziExtension> {
        generateComposePreviewRobolectricTests.apply {
            // 일반 테스트 실행에서는 프리뷰 테스트를 생성·컴파일하지 않는다. 아래 테스트 필터는 그 안전망이다
            enable.set(isRoborazziRun)
            // namespace 는 모듈 build.gradle.kts 에서 이 플러그인 적용 뒤에 정해지므로 늦게 읽는다
            packages.set(provider { listOfNotNull(libraryExtension.namespace) })
            includePrivatePreviews.set(true)
            testerQualifiedClassName.set(PREVIEW_TESTER)
            useScanOptionParametersInTester.set(true)
            annotationFilter.set(AnnotationFilter.Exclude(EXCLUDE_ANNOTATION))
            // Robolectric 4.17 은 compileSdk 37 을 렌더하지 못해 targetSdk 에 맞춘다
            robolectricConfig.set(mapOf("sdk" to "[${libs.versions.targetSdk.get()}]"))
        }
    }

    libraryExtension.testOptions.unitTests.apply {
        isIncludeAndroidResources = true
        all { test ->
            test.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            // SDK 36 샌드박스의 ApplicationSharedMemory.create 가 jdk.internal.access.SharedSecrets 를 쓴다
            test.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
        }
    }

    // Roborazzi 플러그인이 모듈에 직접 선언된 의존성만 검사하므로 전이 의존으로 넘기지 않는다
    dependencies {
        testImplementation(project(":core:testing:android"))
        testImplementation(libs.roborazzi.compose.preview.scanner.support)
        testImplementation(libs.composable.preview.scanner.android)
        testImplementation(libs.robolectric)
        testImplementation(libs.junit4)
        // 생성 테스트가 ComposeContentTestRule 을 직접 참조하는데 scanner-support 가 api 로 넘기지 않는다
        testImplementation(platform(libs.androidx.compose.bom))
        testImplementation(libs.androidx.compose.ui.test.junit4)
    }

    // Robolectric 은 SDK 36 샌드박스를 Java 21 이상에서만 띄운다. 컴파일 툴체인(17)은 그대로 두고 실행 JVM 만 올린다
    val javaToolchains = extensions.getByType<JavaToolchainService>()
    tasks.withType<Test>().configureEach {
        javaLauncher.set(
            javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(ROBOLECTRIC_JAVA_VERSION)) },
        )
    }

    // 생성된 프리뷰 테스트는 렌더가 느리고 기록 기준 없이는 의미가 없어, Roborazzi 태스크를 부를 때만 돌린다.
    // 반대로 Roborazzi 실행에서는 일반 단위 테스트를 빼서, 그 실패가 모듈의 렌더 실패로 잡히지 않게 한다
    tasks.withType<Test>().configureEach {
        filter {
            if (isRoborazziRun) {
                includeTestsMatching("$GENERATED_TEST_CLASS*")
            } else {
                excludeTestsMatching("$GENERATED_TEST_CLASS*")
            }
            isFailOnNoMatchingTests = false
        }
    }
}

/**
 * 명령줄 태스크 이름으로 Roborazzi 실행인지 가른다. 그래서 한 실행에 Roborazzi 태스크와 일반 테스트 태스크를
 * 섞어 부르면 일반 단위 테스트가 조용히 빠지므로 막는다. IDE 에서 생성 테스트 하나만 돌리는 경우는 일부러 지원하지 않는다.
 */
private fun Project.isRoborazziRun(): Boolean {
    val taskNames = gradle.startParameter.taskNames
    val roborazzi = taskNames.any { it.contains("Roborazzi") }
    if (roborazzi) {
        val mixed = taskNames.filter { name -> name.substringAfterLast(':').let { it in PLAIN_TEST_TASKS || it.matches(UNIT_TEST_TASK) } }
        if (mixed.isNotEmpty()) {
            throw GradleException("Roborazzi tasks cannot run with other test tasks $mixed in one invocation. Run them separately.")
        }
    }
    return roborazzi
}

private val PLAIN_TEST_TASKS = setOf("test", "check", "build")
private val UNIT_TEST_TASK = Regex("test\\w*UnitTest")
private const val ROBOLECTRIC_JAVA_VERSION = 21
private const val PREVIEW_TESTER = "com.teamyg.parfait.core.testing.android.UiSnapshotPreviewTester"
private const val EXCLUDE_ANNOTATION = "com.teamyg.parfait.core.designsystem.utils.preview.UiSnapshotExcluded"
private const val GENERATED_TEST_CLASS = "com.github.takahirom.roborazzi.RoborazziPreviewParameterizedTests"
