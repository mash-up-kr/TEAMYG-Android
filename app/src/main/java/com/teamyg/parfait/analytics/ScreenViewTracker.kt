package com.teamyg.parfait.analytics

import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.util.jvm.analytics.Loggers
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

private val logger = Loggers.create("ScreenViewTracker")

/**
 * 화면 진입을 보낼지 판정한다.
 *
 * 스코프와 판정 기준(백스택 크기를 함께 보는 이유)의 근거는
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
