package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Composable
fun YGAlert(
    title: String,
    sub: String,
    hasButton: Boolean,
    buttonText: String = "",
    onButtonClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(YGAtomicColors.Transparency.Black75)
            .padding(horizontal = 20.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = YGTheme.typography.body.b02SB, color = YGAtomicColors.Cherry.Cherry200)
            Text(text = sub, style = YGTheme.typography.body.b02SB, color = YGAtomicColors.Transparency.White75)
        }
        if (hasButton) {
            YGButton(
                text = buttonText,
                buttonType = YGButtonType.XSmall,
                isEnabled = true,
                onClick = onButtonClick,
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
                hasButton = true,
                buttonText = "확인",
                onButtonClick = {},
            )
            YGAlert(
                title = "Title",
                sub = "Sub",
                hasButton = false,
                buttonText = "확인",
                onButtonClick = {},
            )
        }
    }
}
