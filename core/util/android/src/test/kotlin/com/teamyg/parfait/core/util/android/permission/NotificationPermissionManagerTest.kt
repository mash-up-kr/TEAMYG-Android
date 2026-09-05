package com.teamyg.parfait.core.util.android.permission

import android.content.Context
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertTrue

class NotificationPermissionManagerTest {
    // API 33 미만 경로는 이 목을 건드리지 않는다 — 건드리면 android.jar 스텁이 던져 테스트가 깨진다
    private val context: Context = mockk()

    @Test
    fun hasPermission_belowTiramisu_isTrueWithoutCheckingContext() {
        // Given minSdk(26)과 API 32 — POST_NOTIFICATIONS 가 아직 없는 플랫폼이다

        // When 권한 보유를 묻는다
        val atMinSdk = NotificationPermissionManager.hasPermission(context = context, sdkInt = 26)
        val belowTiramisu = NotificationPermissionManager.hasPermission(context = context, sdkInt = 32)

        // Then 허용으로 본다. checkSelfPermission 을 물으면 항상 거부가 나와 판정이 정반대가 된다
        assertTrue(atMinSdk)
        assertTrue(belowTiramisu)
    }
}
