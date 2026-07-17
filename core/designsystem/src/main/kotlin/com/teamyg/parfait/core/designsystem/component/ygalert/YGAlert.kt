package com.teamyg.parfait.core.designsystem.component.ygalert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Composable
fun YGAlert(
    title: String,
    sub: String,
    modifier: Modifier = Modifier,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(YGAtomicColors.Transparency.Black75)
            .padding(
                vertical = YGTheme.layout.padding.padding5,
                horizontal = YGTheme.layout.padding.padding7,
            ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
            modifier = Modifier.weight(1f),
        ) {
            Text(text = title, style = YGTheme.typography.body.b02SB, color = YGAtomicColors.Cherry.Cherry200)
            Text(text = sub, style = YGTheme.typography.body.b02R, color = YGAtomicColors.Transparency.White75)
        }
        if (buttonText != null) {
            YGChipButton(
                text = buttonText,
                colors = YGChipButtonColorsDefaults.CherryBackgroundPressed,
                onClick = onButtonClick ?: {},
            )
        }
    }
}

@Preview
@Composable
private fun YGAlertPreview() {
    YGCustomTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            YGAlert(
                title = "Title",
                sub = "Sub",
                buttonText = "확인",
                onButtonClick = {},
            )
            YGAlert(
                title = "Title",
                sub = "Sub",
            )
        }
    }
}
