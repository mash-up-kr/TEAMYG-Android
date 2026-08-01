package com.teamyg.parfait.core.designsystem.component.ygcanvasdateselect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.shape.canvasCutCornerShape
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Canvas/Button-Date-Select
 */
@Composable
fun YGCanvasDateSelectButton(
    date: String,
    day: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = canvasCutCornerShape()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SizeTokens.Size44.getDp())
            .background(
                color = YGAtomicColors.Transparency.White75,
                shape = shape,
            ).clip(shape)
            .border(
                width = 1.dp,
                color = YGAtomicColors.Gray.Gray500,
                shape = shape,
            ).padding(start = YGTheme.layout.padding.padding6),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date,
                style = YGTheme.typography.body.b02R,
                color = YGAtomicColors.Gray.Gray800,
            )
            Text(
                text = day,
                style = YGTheme.typography.body.b02R,
                color = YGAtomicColors.Gray.Gray300,
            )
        }
        YGIconButton(
            iconResource = R.drawable.ic_calender,
            size = YGIconButtonSize.SIZE_44,
            contentDescription = null,
            onClick = onClick,
        )
    }
}

@YGPreview
@Composable
private fun YGCanvasDateSelectButtonPreview() = PreviewBox {
    YGCanvasDateSelectButton(
        date = "May 20",
        day = "(Wed)",
        onClick = {},
    )
}
