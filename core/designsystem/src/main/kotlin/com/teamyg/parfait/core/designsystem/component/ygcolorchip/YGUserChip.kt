package com.teamyg.parfait.core.designsystem.component.ygcolorchip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygchip.YGColorChip
import com.teamyg.parfait.core.designsystem.component.ygchip.YGColorChipStyle
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors


sealed interface YGUserNameStyle{
    val textStyle: TextStyle
        @Composable get
    val textColor: Color

    data object StyleMedium: YGUserNameStyle{
        override val textStyle: TextStyle
            @Composable
            get() = YGTheme.typography.body.b02R
        override val textColor: Color
            get() = YGAtomicColors.Gray.Gray800
    }

    data object StyleBold: YGUserNameStyle{
        override val textStyle: TextStyle
            @Composable
            get() = YGTheme.typography.body.b02SB
        override val textColor: Color
            get() = YGAtomicColors.Gray.Gray950
    }
}

@Composable
fun YGUserChip(
    colorChipType: YGColorChipType,
    userFirstName: String,
    chip: YGColorChipStyle,
    userName: String,
    userStyle: YGUserNameStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        YGColorChip(
            colorChipType = colorChipType,
            userFirstName = userFirstName,
            chip = chip,
            modifier = modifier,
        )
        Text(
            text = userName,
            color = userStyle.textColor,
            style = userStyle.textStyle
        )
    }

}

@Preview
@Composable
private fun YGUserChipPreview() {
    YGCustomTheme {
        Column {
            YGUserChip(
                colorChipType = YGColorChipType.NametagChip1,
                userFirstName = "문",
                chip = YGColorChipStyle.Style28,
                userName = "안녕하세요 문설빈입니다",
                userStyle = YGUserNameStyle.StyleMedium,
            )

            Spacer(modifier = Modifier.height(15.dp))

            YGUserChip(
                colorChipType = YGColorChipType.NametagChip1,
                userFirstName = "문",
                chip = YGColorChipStyle.Style40,
                userName = "안녕하세요 문설빈입니다",
                userStyle = YGUserNameStyle.StyleBold,
            )
        }
    }
}

