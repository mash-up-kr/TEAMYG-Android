package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

/**
 * Spotlight 토스트 속 닉네임 글자색. 작성자의 Nametag-Chip 타입에 따라 강제 매핑된다 —
 * 칩 자체의 [YGColorChipType.textColor](칩 안 이니셜 글자색)와는 다른, 토스트 전용 색이다.
 *
 * 연핑크(1·2)=Cherry200, 진핑크(3·4)=Cherry300, 체리(5·6)=Cherry400, 그레이(7·8)=White,
 * 멜론(9·10)=Melon, 푸딩(11·12)=Pudding. 컬러를 배정할 수 없는 상태
 * ([YGColorChipType.Default]·[YGColorChipType.NametagChipPlus])는 그레이와 같은 White 로 둔다.
 */
internal fun YGColorChipType.toSpotlightToastNameColor(): Color = when (this) {
    YGColorChipType.NametagChip1, YGColorChipType.NametagChip2 -> YGAtomicColors.Cherry.Cherry200
    YGColorChipType.NametagChip3, YGColorChipType.NametagChip4 -> YGAtomicColors.Cherry.Cherry300
    YGColorChipType.NametagChip5, YGColorChipType.NametagChip6 -> YGAtomicColors.Cherry.Cherry400
    YGColorChipType.NametagChip7, YGColorChipType.NametagChip8 -> YGAtomicColors.Gray.White
    YGColorChipType.NametagChip9, YGColorChipType.NametagChip10 -> YGAtomicColors.Melon.Melon500
    YGColorChipType.NametagChip11, YGColorChipType.NametagChip12 -> YGAtomicColors.Pudding.Pudding500
    YGColorChipType.NametagChipPlus, YGColorChipType.Default -> YGAtomicColors.Gray.White
}
