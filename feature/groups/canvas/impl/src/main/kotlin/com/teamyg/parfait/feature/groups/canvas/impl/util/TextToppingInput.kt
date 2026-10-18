package com.teamyg.parfait.feature.groups.canvas.impl.util

/** 그래핌 클러스터 기준(`String#graphemeClusterCount`) 글자 수 상한 */
const val TEXT_TOPPING_MAX_LENGTH = 100

/** 자동 크기의 최댓값(상한). Text-Area 에 안 들어가면 이 값부터 연속 축소한다 */
internal const val TEXT_TOPPING_FONT_SIZE_SP = 24f

/** 배치 모드 핀치 축소 하한. 사진 토핑의 `TOPPING_MIN_SCALE`(0.05, 상한 없음)과 같은 값으로 맞춘다. */
const val TEXT_TOPPING_MIN_SCALE = 0.05f
