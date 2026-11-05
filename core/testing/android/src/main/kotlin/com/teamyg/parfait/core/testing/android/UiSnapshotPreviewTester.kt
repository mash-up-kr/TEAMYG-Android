package com.teamyg.parfait.core.testing.android

import com.airbnb.lottie.LottieTask
import com.github.takahirom.roborazzi.AndroidComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester.TestParameter.JUnit4TestParameter.AndroidPreviewJUnit4TestParameter
import java.util.concurrent.Executor

/**
 * 로티 파싱은 Compose idle 감지 밖의 스레드에서 끝나 빈 화면이 찍히므로, 렌더 전에 호출 스레드에서 바로 돌게 한다.
 *
 * build-logic `UiSnapshotConfig` 가 이 FQCN 을 문자열로 참조한다. 옮기거나 이름을 바꾸면 그쪽도 함께 바꾼다.
 */
class UiSnapshotPreviewTester private constructor(
    private val delegate: AndroidComposePreviewTester,
) : ComposePreviewTester<AndroidPreviewJUnit4TestParameter> by delegate {
    constructor() : this(AndroidComposePreviewTester())

    // 한 함수에 스냅샷 group 표시가 있으면 그 변형만 남기고, 없으면 그대로 둔다. 파라미터 프리뷰의 값별 항목은 함께 남는다
    override fun testParameters(): List<AndroidPreviewJUnit4TestParameter> = delegate
        .testParameters()
        .groupBy { it.preview.declaringClass to it.preview.methodName }
        .values
        .flatMap { previews ->
            val marked = previews.filter { it.preview.previewInfo.group == UI_SNAPSHOT_PREVIEW_GROUP }
            marked.ifEmpty { previews }
        }.also(::requireParameterNames)

    override fun test(testParameter: AndroidPreviewJUnit4TestParameter) {
        LottieTask.EXECUTOR = ImmediateExecutor
        delegate.test(testParameter)
    }
}

/**
 * 파라미터 프리뷰는 이름이 파일명이 된다. 이름이 없으면 순서 번호로 돌아가 값이 빠질 때 기준 이미지와 어긋난 비교가 나오므로 막는다.
 * 이름은 `core:designsystem` 의 `NamedPreviewParameterProvider` 로 붙인다.
 */
private fun requireParameterNames(parameters: List<AndroidPreviewJUnit4TestParameter>) {
    val parameterized = parameters.map { it.preview }.filter { it.previewIndex != null }
    val unnamed = parameterized.filter { it.previewIndexDisplayName.isNullOrBlank() }
    check(unnamed.isEmpty()) {
        "Preview parameters need names (NamedPreviewParameterProvider): " +
            unnamed.map { "${it.declaringClass}.${it.methodName}" }.distinct()
    }
    val duplicated = parameterized
        .groupBy { Triple(it.declaringClass, it.methodName, it.previewInfo) }
        .filterValues { previews -> previews.map { it.previewIndexDisplayName }.toSet().size != previews.size }
        .keys
    check(duplicated.isEmpty()) {
        "Preview parameter names must be unique per preview: " + duplicated.map { "${it.first}.${it.second}" }
    }
}

/**
 * `core:designsystem` 의 `UI_SNAPSHOT_PREVIEW_GROUP` 과 같은 값이다. 이 모듈이 designsystem 을 의존하지 않아 문자열로 둔다.
 * 바꾸면 그쪽도 함께 바꾼다.
 */
private const val UI_SNAPSHOT_PREVIEW_GROUP = "ui-snapshot"

private val ImmediateExecutor = Executor { command -> command.run() }
