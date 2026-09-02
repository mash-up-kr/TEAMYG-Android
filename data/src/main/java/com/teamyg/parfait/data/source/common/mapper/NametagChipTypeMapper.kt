package com.teamyg.parfait.data.source.common.mapper

import com.teamyg.parfait.domain.model.group.NametagChipType

/**
 * 서버가 주는 칩 이름을 도메인 값으로 바꾼다.
 *
 * 열린 입력이라 모르는 문자열도, 값이 없는 것도 [NametagChipType.DEFAULT] 로 접는다 — 새 타입이
 * 서버에 먼저 들어와도 조회가 통째로 실패하지 않아야 한다.
 *
 * 이 저장소의 다른 서버 유래 enum 이 쓰는 `UNKNOWN` 센티널 대신 기존 값에 흡수하는 쪽을 골랐고,
 * 그 사유와 대가는 `parfait/adr/0024-nametag-chip-unknown-fold.md` 에 있다.
 */
internal fun String?.toNametagChipType(): NametagChipType =
    NametagChipType.entries.firstOrNull { it.name == this } ?: NametagChipType.DEFAULT
