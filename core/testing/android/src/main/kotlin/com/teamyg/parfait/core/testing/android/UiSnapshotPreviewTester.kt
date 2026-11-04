package com.teamyg.parfait.core.testing.android

import com.airbnb.lottie.LottieTask
import com.github.takahirom.roborazzi.AndroidComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester.TestParameter.JUnit4TestParameter.AndroidPreviewJUnit4TestParameter
import java.util.concurrent.Executor

/**
 * 로티 파싱은 Compose idle 감지 밖의 스레드에서 끝나 빈 화면이 찍히므로, 렌더 전에 호출 스레드에서 바로 돌게 한다.
 */
class UiSnapshotPreviewTester private constructor(
    private val delegate: AndroidComposePreviewTester,
) : ComposePreviewTester<AndroidPreviewJUnit4TestParameter> by delegate {
    constructor() : this(AndroidComposePreviewTester())

    override fun test(testParameter: AndroidPreviewJUnit4TestParameter) {
        LottieTask.EXECUTOR = ImmediateExecutor
        delegate.test(testParameter)
    }
}

private val ImmediateExecutor = Executor { command -> command.run() }
