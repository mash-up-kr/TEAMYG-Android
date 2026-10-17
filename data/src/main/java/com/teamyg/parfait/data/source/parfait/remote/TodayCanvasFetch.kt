package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.domain.model.canvas.CanvasVO

sealed interface TodayCanvasFetch {
    data object NotModified : TodayCanvasFetch

    /** @param etag 서버가 헤더를 주지 않았으면 `null` — 다음 요청은 조건 없이 나간다 */
    data class Modified(
        val canvas: CanvasVO,
        val etag: String?,
    ) : TodayCanvasFetch
}
