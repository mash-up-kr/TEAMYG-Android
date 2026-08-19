package com.teamyg.parfait.data.source.common.mapper

import com.teamyg.parfait.domain.model.group.NametagChipType

/**
 * 서버가 주는 칩 이름을 도메인 값으로 바꾼다.
 *
 * 열린 입력이라 모르는 문자열도, 값이 없는 것도 [NametagChipType.DEFAULT] 로 접는다 — 새 타입이
 * 서버에 먼저 들어와도 조회가 통째로 실패하지 않아야 한다.
 *
 * **대가는 서버가 타입을 늘렸을 때 그것이 "반납된 자리"와 구분되지 않는다는 것**이다. 화면이 둘을
 * 같은 중립 색으로 그리고 있어 지금은 보이는 차이가 없고, 이 축에 "값 없음"을 따로 두지 않기로
 * 한 결정이 우선한다.
 */
internal fun String?.toNametagChipType(): NametagChipType =
    NametagChipType.entries.firstOrNull { it.name == this } ?: NametagChipType.DEFAULT
