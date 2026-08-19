package com.teamyg.parfait.feature.groups.setting.impl.util

import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.domain.model.group.NametagChipType

/**
 * 서버가 배정한 칩을 화면 색으로 옮긴다.
 *
 * 값이 없거나 반납된 자리는 [YGColorChipType.Default] 다 — 색이 "그룹 안의 이 사람"을 가리키는
 * 신호라, 가리킬 사람이 없을 때 아무 색이나 돌리면 신호가 거짓이 된다.
 *
 * G-001 목록에도 짝이 되는 변환이 있지만 규칙이 달라(그쪽은 12종을 6종으로 짝지어 접는다)
 * 공용화하지 않는다.
 */
internal fun NametagChipType?.toColorChipType(): YGColorChipType = when (this) {
    NametagChipType.TYPE1 -> YGColorChipType.NametagChip1
    NametagChipType.TYPE2 -> YGColorChipType.NametagChip2
    NametagChipType.TYPE3 -> YGColorChipType.NametagChip3
    NametagChipType.TYPE4 -> YGColorChipType.NametagChip4
    NametagChipType.TYPE5 -> YGColorChipType.NametagChip5
    NametagChipType.TYPE6 -> YGColorChipType.NametagChip6
    NametagChipType.TYPE7 -> YGColorChipType.NametagChip7
    NametagChipType.TYPE8 -> YGColorChipType.NametagChip8
    NametagChipType.TYPE9 -> YGColorChipType.NametagChip9
    NametagChipType.TYPE10 -> YGColorChipType.NametagChip10
    NametagChipType.TYPE11 -> YGColorChipType.NametagChip11
    NametagChipType.TYPE12 -> YGColorChipType.NametagChip12
    NametagChipType.DEFAULT, null -> YGColorChipType.Default
}
