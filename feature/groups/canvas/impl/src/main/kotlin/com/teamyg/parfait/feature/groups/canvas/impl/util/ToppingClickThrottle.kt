package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.os.SystemClock

/**
 * 대상이 바뀌면 즉시 통과시키고, 같은 대상을 다시 누를 때만 창을 적용한다.
 *
 * 대상 개념 없이 게이트 하나로 막으면 토핑을 누른 직후의 바깥 탭까지 씹힌다.
 */
class ToppingClickThrottle(
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) {
    private var lastKey: Any? = null
    private var lastAt = 0L

    fun tryPass(key: Any): Boolean {
        val at = now()
        if (key == lastKey && at - lastAt < windowMillis) return false

        lastKey = key
        lastAt = at
        return true
    }

    companion object {
        /** 판정을 레이어로 옮기면서 기존 연타 방어를 함께 옮겨 온다. */
        private const val DEFAULT_WINDOW_MILLIS = 300L
    }
}
