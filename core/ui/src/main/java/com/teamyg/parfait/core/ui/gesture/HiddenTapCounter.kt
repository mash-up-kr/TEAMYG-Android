package com.teamyg.parfait.core.ui.gesture

/**
 * 숨은 진입점용 연속 탭 카운터. [required]번째 [tap]에서 `true`를 돌려주고 0으로 되돌린다.
 * 시간 제한이 없다 — 느리게 눌러도 세어야 하는 진입점이라 탭 사이 간격으로 막지 않는다.
 */
class HiddenTapCounter(private val required: Int = 5) {
    private var count = 0

    fun tap(): Boolean {
        count += 1
        if (count < required) return false
        count = 0
        return true
    }
}
