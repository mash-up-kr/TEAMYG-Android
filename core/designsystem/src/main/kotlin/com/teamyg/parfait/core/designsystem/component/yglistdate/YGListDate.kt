package com.teamyg.parfait.core.designsystem.component.yglistdate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGChipColorIndicator
import com.teamyg.parfait.core.designsystem.component.ygdatebutton.YGDateButton
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma List-Date
 *
 * Button-Date 가 Disabled 면 Chip-Indicator 는 항상 False (C-201 캘린더 정책 예외 규칙)
 */
@Composable
fun YGListDate(
    text: String,
    isSelected: Boolean,
    isToday: Boolean,
    isEnabled: Boolean,
    isUploaded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
    ) {
        YGDateButton(
            text = text,
            isSelected = isSelected,
            isToday = isToday,
            isEnabled = isEnabled,
            onClick = onClick,
            modifier = Modifier.size(SizeTokens.Size44.getDp()),
        )
        YGChipColorIndicator(isChecked = isEnabled && isUploaded)
    }
}

@YGPreview
@Composable
private fun YGListDatePreview() = PreviewBox {
    Row(
        modifier = Modifier.background(color = Color.White),
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
    ) {
        YGListDate(
            text = "31",
            isSelected = false,
            isToday = false,
            isEnabled = true,
            isUploaded = true,
            onClick = {},
        )
        YGListDate(
            text = "31",
            isSelected = true,
            isToday = false,
            isEnabled = true,
            isUploaded = true,
            onClick = {},
        )
        YGListDate(
            text = "31",
            isSelected = false,
            isToday = true,
            isEnabled = true,
            isUploaded = false,
            onClick = {},
        )
        YGListDate(
            text = "31",
            isSelected = false,
            isToday = false,
            isEnabled = false,
            isUploaded = false,
            onClick = {},
        )
    }
}
