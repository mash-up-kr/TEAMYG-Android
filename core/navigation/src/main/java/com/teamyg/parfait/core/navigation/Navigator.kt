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
