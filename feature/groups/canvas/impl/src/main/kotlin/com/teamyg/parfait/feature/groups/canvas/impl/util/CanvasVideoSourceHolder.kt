package com.teamyg.parfait.feature.groups.canvas.impl.util

/**
 * 캡처한 배경·토핑 스냅샷을 동영상 저장 요청 시점까지 건네는 자리. [CanvasCaptureHolder] 와
 * 같은 이유로 읽으면서 비우지 않는다 — 다음 [put] 이 덮을 때만 갱신된다.
 */
internal object CanvasVideoSourceHolder {
    @Volatile
    private var snapshot: CanvasVideoSourceSnapshot? = null

    fun put(value: CanvasVideoSourceSnapshot) {
        snapshot = value
    }

    fun peek(): CanvasVideoSourceSnapshot? = snapshot
}
