package com.teamyg.parfait.core.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.scopes.ActivityRetainedScoped

@ActivityRetainedScoped
class Navigator(initialNavigationKey: NavKey) {
    private val _backStack: SnapshotStateList<NavKey> = mutableStateListOf(initialNavigationKey)
    val backStack: List<NavKey> get() = _backStack

    fun goTo(destination: NavKey) {
        _backStack.add(destination)
    }

    /**
     * [destination] 을 백스택 최상단으로 올린다.
     *
     * 이미 백스택에 있으면 그 위에 쌓여 있던 키들을 모두 걷어내 기존 화면을 그대로 재사용하고,
     * 없으면 평범하게 새로 쌓는다.
     */
    fun goToSingleClearTop(destination: NavKey) {
        val destinationIndex = _backStack.lastIndexOf(destination)

        if (destinationIndex == -1) {
            _backStack.add(destination)
            return
        }

        // 하나씩 걷어내면 스냅샷에도 그만큼 변경이 쌓이므로 한 번에 잘라낸다
        _backStack.removeRange(destinationIndex + 1, _backStack.size)
    }

    fun onBack() {
        if (_backStack.size <= 1) {
            // ResultEffect 발동 상황에서 사이즈가 1인 경우 크래시 발생
            return
        }

        _backStack.removeLastOrNull()
    }

    fun clearBackStack() {
        _backStack.clear()
    }
}
