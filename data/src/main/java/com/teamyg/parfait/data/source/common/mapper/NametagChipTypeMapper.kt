package com.teamyg.parfait.data.source.common.mapper

import com.teamyg.parfait.domain.model.group.NametagChipType

/**
 * 서버가 주는 칩 이름을 도메인 값으로 바꾼다.
 *
 * 열린 입력이라 모르는 문자열은 `null` 로 접는다 — 새 타입이 서버에 먼저 들어와도 조회가 통째로
 * 실패하지 않아야 한다. `"DEFAULT"` 는 모르는 값이 아니므로 접지 않는다.
 */
internal fun String?.toNametagChipType(): NametagChipType? =
    this?.let { raw -> NametagChipType.entries.firstOrNull { it.name == raw } }
