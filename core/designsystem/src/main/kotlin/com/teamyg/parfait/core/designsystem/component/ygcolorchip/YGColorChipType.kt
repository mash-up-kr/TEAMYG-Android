package com.teamyg.parfait.core.designsystem.component.ygcolorchip

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

sealed interface YGColorChipType {
    val fillColor: Color
    val strokeColor: Color
    val textColor: Color

    data object nametagChip1 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry100
        override val strokeColor = YGAtomicColors.Cherry.Cherry50
        override val textColor = YGAtomicColors.Cherry.Cherry300
    }
    data object nametagChip2 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry200
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Melon.Melon500
    }
    data object nametagChip3 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry400
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Melon.Melon500
    }
    data object nametagChip4 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry300
        override val strokeColor = YGAtomicColors.Cherry.Cherry50
        override val textColor = YGAtomicColors.Pudding.Pudding500
    }
    data object nametagChip5 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry500
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Cherry.Cherry100
    }
    data object nametagChip6 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry700
        override val strokeColor = YGAtomicColors.Cherry.Cherry200
        override val textColor = YGAtomicColors.Gray.White
    }
    data object nametagChip7 : YGColorChipType {
        override val fillColor = YGAtomicColors.Gray.White
        override val strokeColor = YGAtomicColors.Cherry.Cherry200
        override val textColor = YGAtomicColors.Melon.Melon500
    }
    data object nametagChip8 : YGColorChipType {
        override val fillColor = YGAtomicColors.Gray.Gray200
        override val strokeColor = YGAtomicColors.Cherry.Cherry200
        override val textColor = YGAtomicColors.Pudding.Pudding500
    }
    data object nametagChip9 : YGColorChipType {
        override val fillColor = YGAtomicColors.Melon.Melon500
        override val strokeColor = YGAtomicColors.Cherry.Cherry50
        override val textColor = YGAtomicColors.Cherry.Cherry50
    }
    data object nametagChip10 : YGColorChipType {
        override val fillColor = YGAtomicColors.Melon.Melon500
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Cherry.Cherry300
    }
    data object nametagChip11 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry400
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Melon.Melon500
    }
    data object nametagChip12 : YGColorChipType {
        override val fillColor = YGAtomicColors.Pudding.Pudding500
        override val strokeColor = YGAtomicColors.Melon.Melon500
        override val textColor = YGAtomicColors.Cherry.Cherry300
    }
    data object nametagChip13 : YGColorChipType {
        override val fillColor = YGAtomicColors.Pudding.Pudding500
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Cherry.Cherry300
    }
    data object nametagChipPlus : YGColorChipType {
        override val fillColor = YGAtomicColors.Gray.White
        override val strokeColor = YGAtomicColors.Gray.Gray100
        override val textColor = YGAtomicColors.Gray.Gray900
    }


}
