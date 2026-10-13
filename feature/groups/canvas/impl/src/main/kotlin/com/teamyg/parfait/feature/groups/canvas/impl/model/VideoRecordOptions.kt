package com.teamyg.parfait.feature.groups.canvas.impl.model

import com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO

/** 타임랩스 영상의 산출물 규격과 녹화 한도 */
internal object VideoRecordOptions {
    const val WIDTH_PX: Int = 1080

    const val HEIGHT_PX: Int = (WIDTH_PX / CANVAS_AREA_ASPECT_RATIO).toInt()

    /** 이미지 로드를 기다리는 동안 닫기와 뒤로가기가 막혀 있어, 상한이 없으면 화면에 갇힌다 */
    const val SOURCE_LOAD_TIMEOUT_MILLIS: Long = 15_000L
}
