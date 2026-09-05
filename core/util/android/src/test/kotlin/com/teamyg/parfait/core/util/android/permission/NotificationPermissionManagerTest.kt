package com.teamyg.parfait.core.util.android.permission

import android.content.Context
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPermissionManagerTest {
    // API 33 미만 경로는 이 목을 건드리지 않는다 — 건드리면 android.jar 스텁이 던져 테스트가 깨진다
    private val context: Context = mockk()

    @Test
    fun isRuntimePermissionRequired_belowTiramisu_isFalse() {
        // POST_NOTIFICATIONS 가 아직 없는 플랫폼이다
        assertFalse(NotificationPermissionManager.isRuntimePermissionRequired(sdkInt = 26))
        assertFalse(NotificationPermissionManager.isRuntimePermissionRequired(sdkInt = 32))
    }

    @Test
    fun isRuntimePermissionRequired_tiramisuAndAbove_isTrue() {
        // 런타임 권한으로 물어야 한다
        assertTrue(NotificationPermissionManager.isRuntimePermissionRequired(sdkInt = 33))
        assertTrue(NotificationPermissionManager.isRuntimePermissionRequired(sdkInt = 36))
    }

    @Test
    fun hasPermission_belowTiramisu_isTrueWithoutCheckingContext() {
        // Given API 32 기기 — 알림은 OS 설정에서 기본으로 켜져 있다

        // When 권한 보유를 묻는다
        val result = NotificationPermissionManager.hasPermission(context = context, sdkInt = 32)

        // Then 허용으로 본다. checkSelfPermission 을 물으면 항상 거부가 나와 정반대가 된다
        assertTrue(result)
    }
}
