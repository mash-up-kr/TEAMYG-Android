package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.domain.model.canvas.CanvasVO

/** 빈 캔버스 안내 연출을 정하는 "빈 캔버스". 그룹·혼자 공간 ViewModel 이 같은 조건을 쓴다 */
internal fun CanvasVO.isEmptyForIntro(): Boolean = toppings.isEmpty() && texts.isEmpty() && background == null
