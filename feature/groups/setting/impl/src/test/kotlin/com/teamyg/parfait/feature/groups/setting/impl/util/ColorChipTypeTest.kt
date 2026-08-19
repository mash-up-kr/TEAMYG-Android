package com.teamyg.parfait.feature.groups.setting.impl.util

import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.domain.model.group.NametagChipType
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorChipTypeTest {
    @Test
    fun toColorChipType_pairsAllTwelveNametagTypesOneToOne() {
        // Given Nametag 타입 12종은 각각 자기 자리의 색 칩 하나에만 대응한다
        val pairs = listOf(
            NametagChipType.TYPE1 to YGColorChipType.NametagChip1,
            NametagChipType.TYPE2 to YGColorChipType.NametagChip2,
            NametagChipType.TYPE3 to YGColorChipType.NametagChip3,
            NametagChipType.TYPE4 to YGColorChipType.NametagChip4,
            NametagChipType.TYPE5 to YGColorChipType.NametagChip5,
            NametagChipType.TYPE6 to YGColorChipType.NametagChip6,
            NametagChipType.TYPE7 to YGColorChipType.NametagChip7,
            NametagChipType.TYPE8 to YGColorChipType.NametagChip8,
            NametagChipType.TYPE9 to YGColorChipType.NametagChip9,
            NametagChipType.TYPE10 to YGColorChipType.NametagChip10,
            NametagChipType.TYPE11 to YGColorChipType.NametagChip11,
            NametagChipType.TYPE12 to YGColorChipType.NametagChip12,
        )

        // When/Then 짝이 그대로 맞는다
        pairs.forEach { (nametag, colorChip) ->
            assertEquals(colorChip, nametag.toColorChipType())
        }
    }

    @Test
    fun toColorChipType_releasedFallsBackToDefault() {
        // Given 마지막 토퍼가 그룹을 나가 자리가 반납됐다
        // When/Then 나간 사람 색을 계속 쓰지 않고 중립으로 간다
        assertEquals(YGColorChipType.Default, NametagChipType.RELEASED.toColorChipType())
    }

    @Test
    fun toColorChipType_missingFallsBackToDefault() {
        // Given 아직 아무도 토핑을 올리지 않아 칩이 없다
        val missing: NametagChipType? = null

        // When/Then 아무 색이나 돌리지 않는다
        assertEquals(YGColorChipType.Default, missing.toColorChipType())
    }
}
