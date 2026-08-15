package com.teamyg.parfait.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
