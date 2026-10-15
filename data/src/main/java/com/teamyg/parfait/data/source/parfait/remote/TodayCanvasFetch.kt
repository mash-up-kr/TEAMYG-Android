package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.domain.model.canvas.CanvasVO

sealed interface TodayCanvasFetch {
    /** 보낸 ETag 가 아직 유효하다. 캐시의 값이 최신이다 */
    data object NotModified : TodayCanvasFetch

    /** @param etag 서버가 헤더를 주지 않았으면 `null` — 다음 요청은 조건 없이 나간다 */
    data class Modified(
        val canvas: CanvasVO,
        val etag: String?,
    ) : TodayCanvasFetch
}
