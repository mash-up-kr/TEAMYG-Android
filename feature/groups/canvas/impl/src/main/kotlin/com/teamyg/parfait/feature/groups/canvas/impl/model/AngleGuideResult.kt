package com.teamyg.parfait.feature.groups.canvas.impl.model

/**
 * @param effectiveValue 저장·렌더에 쓰는 값 — 스냅 중엔 가장 가까운 90도 배수로 고정된다.
 * @param isHorizontal 스냅 대상이 0/180도(가로선)인지, 90/270도(세로선)인지.
 */
internal data class AngleGuideResult(
    val isSnapped: Boolean,
    val effectiveValue: Float,
    val isHorizontal: Boolean,
)
