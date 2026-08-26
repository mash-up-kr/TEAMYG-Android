package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.compose.ui.unit.DpOffset

/** 어떤 사각형(토핑 자신 또는 그 스트로크)의 회전이 반영된 네 꼭짓점. 캔버스 기준 절대 좌표. */
data class ToppingCorners(
    val topLeft: DpOffset,
    val topRight: DpOffset,
    val bottomLeft: DpOffset,
    val bottomRight: DpOffset,
)
