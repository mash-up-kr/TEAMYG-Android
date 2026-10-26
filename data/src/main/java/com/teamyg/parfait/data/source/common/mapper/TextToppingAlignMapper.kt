package com.teamyg.parfait.data.source.common.mapper

import com.teamyg.parfait.domain.model.topping.TextToppingAlign

/** 모르는 값은 서버 기본값인 가운데로 읽는다 — 정렬이 늘어도 캔버스 조회가 통째로 실패하지 않아야 한다 */
internal fun String.toTextToppingAlign(): TextToppingAlign =
    TextToppingAlign.entries.firstOrNull { it.name == this } ?: TextToppingAlign.CENTER
