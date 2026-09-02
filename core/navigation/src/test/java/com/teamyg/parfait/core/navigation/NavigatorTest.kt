package com.teamyg.parfait.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private data object Home : NavKey

private data object Detail : NavKey

private data object Login : NavKey

class NavigatorTest {
    private fun navigator() = Navigator(initialNavigationKey = Home)

    @Test
    fun replaceAll_stackHasSeveralEntries_leavesOnlyTheDestination() {
        // Given 여러 화면이 쌓인 백스택
        val navigator = navigator()
        navigator.goTo(Detail)
        assertEquals(listOf(Home, Detail), navigator.backStack)

        // When 되돌아갈 곳 없이 전환한다
        navigator.replaceAll(Login)

        // Then 목적지 하나만 남는다
        assertEquals(listOf(Login), navigator.backStack)
    }

    @Test
    fun replaceAll_always_leavesStackNonEmpty() {
        // Given 어떤 상태의 백스택이든
        val navigator = navigator()

        // When 전환한다
        navigator.replaceAll(Login)

        // Then 백스택이 비지 않는다 — 비우기와 채우기를 나눠 노출하지 않는 이유가 이것이다.
        // 빈 백스택은 `onBack` 이 이미 방어하고 있는 크래시 원인이다
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun onBack_afterReplaceAll_staysOnTheDestination() {
        // Given 전환으로 목적지 하나만 남은 상태
        val navigator = navigator()
        navigator.goTo(Detail)
        navigator.replaceAll(Login)

        // When 뒤로 간다
        navigator.onBack()

        // Then 이전 화면으로 돌아가지 않는다 — 걷어낸 화면은 되살아나지 않아야 한다
        assertEquals(listOf(Login), navigator.backStack)
    }

    @Test
    fun popUpTo_targetIsBelowSeveralEntries_removesEverythingAboveIt() {
        // Given 목적지 위로 두 화면이 쌓인 백스택
        val navigator = navigator()
        navigator.goTo(Detail)
        navigator.goTo(Login)

        // When 목적지 타입까지 걷어낸다
        val reached = navigator.popUpTo<Home>()

        // Then 목적지만 남고 도달했다고 알린다
        assertTrue(reached)
        assertEquals(listOf(Home), navigator.backStack)
    }

    @Test
    fun popUpTo_targetIsAlreadyOnTop_leavesTheStackAlone() {
        // Given 목적지가 이미 최상단인 백스택
        val navigator = navigator()

        // When 같은 타입까지 걷어낸다
        val reached = navigator.popUpTo<Home>()

        // Then 걷어낼 것이 없으므로 그대로다. 도달 여부는 참이다 —
        // 호출부가 보기에 "그 화면에 있게 됐다"는 결과가 같다
        assertTrue(reached)
        assertEquals(listOf(Home), navigator.backStack)
    }

    @Test
    fun popUpTo_targetIsNotInTheStack_changesNothing() {
        // Given 목적지 타입이 없는 백스택
        val navigator = navigator()
        navigator.goTo(Detail)

        // When 없는 타입까지 걷어내려 한다
        val reached = navigator.popUpTo<Login>()

        // Then 백스택을 건드리지 않고 실패를 알린다 — 못 찾았는데 비우면
        // 사용자가 어디에도 없는 화면에 남는다
        assertFalse(reached)
        assertEquals(listOf(Home, Detail), navigator.backStack)
    }
}
