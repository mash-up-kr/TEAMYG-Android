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

    /** [destination] 으로 가면서 지금 화면은 백스택에서 걷어낸다. 뒤로 가면 지금 화면을 건너뛴다 */
    fun goToAndPopCurrent(destination: NavKey) {
        val currentIndex = _backStack.lastIndex
        if (currentIndex < 0) {
            _backStack.add(destination)
            return
        }

        _backStack[currentIndex] = destination
    }

    /**
     * 백스택을 비우고 [destination] 하나만 남긴다. **되돌아갈 곳이 없어야 하는 전환**에 쓴다 —
     * 로그인 완료·강제 로그아웃처럼 이전 화면으로 돌아가는 것이 말이 안 되는 경우다.
     *
     * 비우기와 채우기를 한 함수로 묶은 이유: 둘을 따로 노출하면 그 사이에 **백스택이 빈 상태**가
     * 생기고, 채우는 것은 호출부의 규약일 뿐 강제되지 않는다. 빈 백스택은 [onBack] 이 이미
     * 방어하고 있는 크래시 원인이라 아예 만들 수 없게 막는다.
     */
    fun replaceAll(destination: NavKey) {
        _backStack.clear()
        _backStack.add(destination)
    }

    fun onBack() {
        if (_backStack.size <= 1) {
            // ResultEffect 발동 상황에서 사이즈가 1인 경우 크래시 발생
            return
        }

        _backStack.removeLastOrNull()
    }
}
