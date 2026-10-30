package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.compose.ui.unit.dp

/** 그래핌 클러스터 기준(`String#graphemeClusterCount`) 글자 수 상한 */
internal const val TEXT_TOPPING_MAX_LENGTH = 100

/** 자동 크기의 최댓값(디자인 단위) */
internal const val TEXT_TOPPING_FONT_SIZE_SP = 24f

/** 디자인 기준 캔버스 너비. 디자인 값 × (실제 캔버스 너비 / 이 값)으로 그린다 */
internal val TEXT_TOPPING_DESIGN_CANVAS_WIDTH = 335.dp

/** 배치 모드 핀치 축소 하한. 사진 토핑의 `TOPPING_MIN_SCALE`(0.05, 상한 없음)과 같은 값으로 맞춘다. */
internal const val TEXT_TOPPING_MIN_SCALE = 0.05f
