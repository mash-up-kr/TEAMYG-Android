package com.teamyg.parfait.core.designsystem.component.ygcolorchip

import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

sealed interface YGColorChipType {
    val fillColor: Color
    val strokeColor: Color
    val textColor: Color

    data object NametagChip1 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry100
        override val strokeColor = YGAtomicColors.Cherry.Cherry50
        override val textColor = YGAtomicColors.Cherry.Cherry300
    }

    data object NametagChip2 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry200
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Melon.Melon500
    }

    data object NametagChip3 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry400
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Melon.Melon500
    }

    data object NametagChip4 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry300
        override val strokeColor = YGAtomicColors.Cherry.Cherry50
        override val textColor = YGAtomicColors.Pudding.Pudding500
    }

    data object NametagChip5 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry500
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Cherry.Cherry100
    }

    data object NametagChip6 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry700
        override val strokeColor = YGAtomicColors.Cherry.Cherry200
        override val textColor = YGAtomicColors.Gray.White
    }

    data object NametagChip7 : YGColorChipType {
        override val fillColor = YGAtomicColors.Gray.White
        override val strokeColor = YGAtomicColors.Cherry.Cherry200
        override val textColor = YGAtomicColors.Melon.Melon500
    }

    data object NametagChip8 : YGColorChipType {
        override val fillColor = YGAtomicColors.Gray.Gray200
        override val strokeColor = YGAtomicColors.Cherry.Cherry200
        override val textColor = YGAtomicColors.Pudding.Pudding500
    }

    data object NametagChip9 : YGColorChipType {
        override val fillColor = YGAtomicColors.Melon.Melon500
        override val strokeColor = YGAtomicColors.Cherry.Cherry50
        override val textColor = YGAtomicColors.Pudding.Pudding500
    }

    data object NametagChip10 : YGColorChipType {
        override val fillColor = YGAtomicColors.Melon.Melon500
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Cherry.Cherry300
    }

    data object NametagChip11 : YGColorChipType {
        override val fillColor = YGAtomicColors.Pudding.Pudding500
        override val strokeColor = YGAtomicColors.Melon.Melon500
        override val textColor = YGAtomicColors.Cherry.Cherry300
    }

    data object NametagChip12 : YGColorChipType {
        override val fillColor = YGAtomicColors.Pudding.Pudding500
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Cherry.Cherry300
    }

    // 5명이상 시 + 컬러칩 타입
    data object NametagChipPlus : YGColorChipType {
        override val fillColor = YGAtomicColors.Gray.White
        override val strokeColor = YGAtomicColors.Gray.Gray100
        override val textColor = YGAtomicColors.Gray.Gray900
    }

    /**
     * 실제 컬러를 배정할 수 없을 때 쓰는 중립 상태 — 탈퇴한 그룹원의 칩을 보여줘야 하거나,
     * 칩 정보를 불러오지 못했을 때 등.
     */
    data object Default : YGColorChipType {
        override val fillColor = YGAtomicColors.Gray.White
        override val strokeColor = YGAtomicColors.Gray.Gray100
        override val textColor = YGAtomicColors.Gray.Gray300
    }
}
