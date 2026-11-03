package com.teamyg.parfait.data.repository.notification

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSettingRepositoryImplTest {
    private val repository = NotificationSettingRepositoryImpl()

    @Test
    fun getNotificationSetting_initially_returnsMockValues() = runTest {
        // When 처음 조회한다
        val setting = repository.getNotificationSetting().getOrThrow()

        // Then mock 초기값이 내려온다
        assertTrue(setting.isCanvasNotificationEnabled)
        assertFalse(setting.isActivityNotificationEnabled)
    }

    @Test
    fun updateCanvasNotification_changesOnlyCanvasValue() = runTest {
        // When 캔버스 알림을 끈다
        repository.updateCanvasNotification(enabled = false)

        // Then 다음 조회에 캔버스 값만 바뀌어 있다
        val setting = repository.getNotificationSetting().getOrThrow()
        assertFalse(setting.isCanvasNotificationEnabled)
        assertFalse(setting.isActivityNotificationEnabled)
    }

    @Test
    fun updateActivityNotification_changesOnlyActivityValue() = runTest {
        // When 활동 알림을 켠다
        repository.updateActivityNotification(enabled = true)

        // Then 다음 조회에 활동 값만 바뀌어 있다
        val setting = repository.getNotificationSetting().getOrThrow()
        assertTrue(setting.isCanvasNotificationEnabled)
        assertTrue(setting.isActivityNotificationEnabled)
    }
}
