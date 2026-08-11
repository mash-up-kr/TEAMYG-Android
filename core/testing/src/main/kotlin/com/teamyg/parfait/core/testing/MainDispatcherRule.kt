package com.teamyg.parfait.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * `Dispatchers.Main` 을 테스트 디스패처로 바꾼다.
 *
 * [dispatcher] 를 **공개**하는 이유: `runTest` 를 인자 없이 부르면 자기
 * `TestCoroutineScheduler` 를 새로 만들어서 `advanceUntilIdle()` 이 Main 쪽 큐를
 * 비우지 못한다. 호출부는 `runTest(mainDispatcherRule.dispatcher)` 로 명시 전달해
 * 스케줄러를 하나로 묶어야 한다.
 *
 * 기본값이 [StandardTestDispatcher] 인 이유: `UnconfinedTestDispatcher` 는 즉시
 * 디스패치라 실행 순서 버그를 감춘다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
