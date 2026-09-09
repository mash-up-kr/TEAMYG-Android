package com.teamyg.parfait.analytics

import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.util.jvm.analytics.Loggers
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

private val logger = Loggers.create("ScreenViewTracker")

/**
 * 화면 진입을 보낼지 판정한다.
 *
 * 마지막 전송 값을 컴포지션 밖에 들기 위해 `@ActivityRetainedScoped` 다. 수집기가 사는
 * `LaunchedEffect` 는 Activity 가 재생성되면 다시 시작하고, 그때 이동이 없었는데도
 * 같은 상태가 한 번 더 방출된다.
 *
 * 판정에 백스택 크기가 함께 들어가는 이유는 같은 값의 키가 겹쳐 쌓일 수 있어서다
 * (`Navigator#goTo` 는 조건 없이 더한다). 근거는
 * `parfait/adr/0031-analytics-central-screen-mapping.md`.
 */
@ActivityRetainedScoped
class ScreenViewTracker @Inject constructor(
    private val analyticsLogger: AnalyticsLogger,
) {
    private var lastTracked: Pair<Int, NavKey?>? = null

    fun track(
        backStackSize: Int,
        top: NavKey?,
    ) {
        val current = backStackSize to top
        if (current == lastTracked) return
        lastTracked = current

        if (top == null) return

        val screen = top.toAnalyticsScreenOrNull()
        if (screen == null) {
            logger.w { "화면 ID 매핑이 없다: ${top.javaClass.name}" }
            return
        }

        analyticsLogger.logScreenView(screen)
    }
}
