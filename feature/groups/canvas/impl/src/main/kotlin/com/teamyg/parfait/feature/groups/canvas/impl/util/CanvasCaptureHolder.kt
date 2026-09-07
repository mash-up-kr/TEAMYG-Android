package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.graphics.Bitmap

/**
 * 캡처한 캔버스를 저장 미리보기로 건네는 자리. 왜 파일이 아닌지는
 * `parfait/specs/2026-09-07-canvas-save-preview-capture-holder.md` 에 있다.
 *
 * ⚠️ 읽으면서 비우지 않는다. 미리보기는 화면에서 사라지지 않고도 다시 컴포즈되고(Activity
 * 재생성·백스택 하강 후 복귀), 그때 비어 있으면 보고 있던 그림을 잃는다.
 */
internal object CanvasCaptureHolder {
    @Volatile
    private var captured: Bitmap? = null

    fun put(bitmap: Bitmap) {
        captured = bitmap
    }

    fun peek(): Bitmap? = captured

    /** 테스트가 전역 상태를 되돌리는 수단. 앱 코드에서 부르는 곳은 두지 않는다. */
    fun clear() {
        captured = null
    }
}
