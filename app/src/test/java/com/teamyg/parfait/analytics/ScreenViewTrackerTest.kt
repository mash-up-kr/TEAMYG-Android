package com.teamyg.parfait.analytics

import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeAnalyticsLogger : AnalyticsLogger {
    val screenViews = mutableListOf<AnalyticsScreen>()

    override fun setCollectionEnabled(enabled: Boolean) = Unit

    override fun setUserProperty(
        name: String,
        value: String,
    ) = Unit

    override fun logScreenView(screen: AnalyticsScreen) {
        screenViews += screen
    }
}

class ScreenViewTrackerTest {
    private val logger = FakeAnalyticsLogger()
    private val tracker = ScreenViewTracker(logger)

    @Test
    fun track_sameSizeAndKeyTwice_logsOnce() {
        // Given, When 컴포지션이 죽었다 살아나 같은 상태를 다시 방출한 경우
        tracker.track(backStackSize = 1, top = NavKeyGroupList)
        tracker.track(backStackSize = 1, top = NavKeyGroupList)

        // Then 이동이 없었으므로 한 번만 나간다
        assertEquals(listOf("G-001"), logger.screenViews.map { it.screenId })
    }

    @Test
    fun track_sameKeyPushedOnTopOfItself_logsAgain() {
        // Given, When 그룹 목록에서 그룹 목록 딥링크를 탭해 같은 값이 겹쳐 쌓인 경우.
        // Navigator.goTo 는 조건 없이 add 한다
        tracker.track(backStackSize = 1, top = NavKeyGroupList)
        tracker.track(backStackSize = 2, top = NavKeyGroupList)

        // Then 화면은 실제로 바뀌었으므로 두 번 나간다
        assertEquals(listOf("G-001", "G-001"), logger.screenViews.map { it.screenId })
    }

    @Test
    fun track_backToPreviousScreen_logsThatScreenAgain() {
        // Given, When 목록 → 캔버스 → 뒤로 가기
        tracker.track(backStackSize = 1, top = NavKeyGroupList)
        tracker.track(backStackSize = 2, top = NavKeyCanvasMain(groupId = 1L))
        tracker.track(backStackSize = 1, top = NavKeyGroupList)

        // Then 복귀도 조회로 센다
        assertEquals(
            listOf("G-001", "C-001", "G-001"),
            logger.screenViews.map { it.screenId },
        )
    }

    @Test
    fun track_unmappedKey_logsNothing() {
        // Given 매핑에 없는 키
        val unmapped = object : NavKey {}

        // When
        tracker.track(backStackSize = 1, top = unmapped)

        // Then 아무 ID 나 보내지 않는다
        assertEquals(emptyList(), logger.screenViews)
    }

    @Test
    fun track_nullTop_logsNothing() {
        // Given, When 백스택이 빈 순간
        tracker.track(backStackSize = 0, top = null)

        // Then
        assertEquals(emptyList(), logger.screenViews)
    }
}
