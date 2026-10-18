package com.teamyg.parfait.deeplink

import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
import com.teamyg.parfait.feature.intro.api.NavKeySplash
import com.teamyg.parfait.feature.intro.api.NavKeyTermAgree
import com.teamyg.parfait.feature.login.api.NavKeyLogin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AwaitAppLinkReadyTest {
    @Test
    fun awaitAppLinkReady_whileSplash_waitsAndChecksSessionOnlyAfterLeavingSplash() = runTest {
        // Given 스플래시가 아직 목적지를 확정하지 않았다
        val top = MutableStateFlow<NavKey?>(NavKeySplash)
        var sessionChecks = 0
        var navigable = false
        launch {
            awaitAppLinkReady(
                currentNavKey = top,
                hasActiveSession = {
                    sessionChecks++
                    true
                },
            )
            navigable = true
        }

        // When
        advanceUntilIdle()

        // Then 스플래시의 replaceAll 에 지워지니 쌓지 않고, 부트스트랩이 토큰을 지우기 전이니 세션도 묻지 않는다
        assertFalse(navigable)
        assertEquals(0, sessionChecks)

        // When 스플래시가 캔버스로 목적지를 확정한다
        top.value = NavKeyCanvasMain(groupId = 1)
        advanceUntilIdle()

        // Then 그제야 세션을 한 번 확인하고 통과한다
        assertTrue(navigable)
        assertEquals(1, sessionChecks)
    }

    @Test
    fun awaitAppLinkReady_activeSession_proceedsOnCurrentScreen() = runTest {
        // Given 로그인된 유저가 캔버스를 보고 있다
        val top = MutableStateFlow<NavKey?>(NavKeyCanvasMain(groupId = 1))
        var navigable = false
        launch {
            awaitAppLinkReady(currentNavKey = top, hasActiveSession = { true })
            navigable = true
        }

        // When
        advanceUntilIdle()

        // Then 그룹 목록을 기다리지 않고 바로 쌓는다
        assertTrue(navigable)
    }

    @Test
    fun awaitAppLinkReady_noSession_waitsUntilGroupListOnTop() = runTest {
        // Given 로그인 전에 링크가 왔다
        val top = MutableStateFlow<NavKey?>(NavKeyLogin)
        var navigable = false
        launch {
            awaitAppLinkReady(currentNavKey = top, hasActiveSession = { false })
            navigable = true
        }
        advanceUntilIdle()
        assertFalse(navigable)

        // When 신규 유저가 약관 동의 화면으로 넘어간다
        top.value = NavKeyTermAgree(registrationToken = "token")
        advanceUntilIdle()

        // Then 아직 쌓지 않는다 — 약관 동의가 끝날 때의 replaceAll 에 지워진다
        assertFalse(navigable)

        // When 가입이 끝나 그룹 목록이 맨 위가 된다
        top.value = NavKeyGroupList
        advanceUntilIdle()

        // Then 그제야 쌓는다
        assertTrue(navigable)
    }
}
