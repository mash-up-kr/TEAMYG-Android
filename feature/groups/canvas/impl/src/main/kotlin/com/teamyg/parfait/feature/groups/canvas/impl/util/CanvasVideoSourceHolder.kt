package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasVideoSource

/**
 * 캔버스 메인이 저장 미리보기로 영상 재료를 건네는 자리. [CanvasCaptureHolder] 와 수명이 같다.
 *
 * 홀더를 쓰는 이유: 미리보기가 떠 있는 동안 캔버스 메인은 컴포지션에서 빠져 있어 그쪽 캔버스를
 * 읽을 수 없고, 토핑 목록은 NavKey 에 싣기에 크다.
 *
 * ⚠️ 읽으면서 비우지 않는다. 이유는 [CanvasCaptureHolder] 와 같다. 프로세스가 죽었다 살아나면
 * 비어 있고, 그때 영상 저장은 실패로 끝난다.
 */
internal object CanvasVideoSourceHolder {
    @Volatile
    private var source: CanvasVideoSource? = null

    fun put(source: CanvasVideoSource) {
        this.source = source
    }

    fun peek(): CanvasVideoSource? = source

    fun clear() {
        source = null
    }
}
