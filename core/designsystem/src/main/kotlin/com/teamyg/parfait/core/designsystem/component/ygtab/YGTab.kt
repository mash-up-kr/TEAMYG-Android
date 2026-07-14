package com.teamyg.parfait.core.designsystem.component.ygtab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygtogglebutton.YGToggleButton
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Composable
fun YGTab(
    startLayout: @Composable () -> Unit,
    endLayout: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = YGAtomicColors.Transparency.White50,
                shape = YGTheme.shapes.radius.round,
            ).border(
                width = 1.dp,
                color = YGAtomicColors.Transparency.Black5,
                shape = YGTheme.shapes.radius.round,
            ).padding(all = YGTheme.layout.padding.padding2),
    ) {
        startLayout()
        endLayout()
    }
}

@Preview
@Composable
fun YGTabPreview() {
    YGCustomTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            YGTab(
                startLayout = {
                    YGToggleButton(
                        text = "Parfait",
                        isSelected = true,
                        onClick = {},
                        iconResource = R.drawable.ic_plus,
                    )
                },
                endLayout = {
                    YGToggleButton(
                        text = "Parfait",
                        isSelected = false,
                        onClick = {},
                    )
                },
            )

            YGTab(
                startLayout = {
                    YGToggleButton(
                        text = "Parfait",
                        isSelected = false,
                        onClick = {},
                        iconResource = R.drawable.ic_plus,
                    )
                },
                endLayout = {
                    YGToggleButton(
                        text = "Parfait",
                        isSelected = true,
                        onClick = {},
                    )
                },
            )
        }
    }
}
