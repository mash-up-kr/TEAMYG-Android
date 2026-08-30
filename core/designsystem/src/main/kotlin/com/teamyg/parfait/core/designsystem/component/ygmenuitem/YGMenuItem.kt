package com.teamyg.parfait.core.designsystem.component.ygmenuitem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple

/**
 * Figma Menu-Item
 */
@Composable
fun YGMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResource: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val shape = YGTheme.shapes.radius.none
    val backgroundColor = if (isPressed) {
        YGAtomicColors.Gray.White
    } else {
        YGAtomicColors.Transparency.White75
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SizeTokens.Size44.getDp())
            .background(
                color = backgroundColor,
                shape = shape,
            ).clip(shape)
            .border(
                width = 1.dp,
                color = YGAtomicColors.Gray.Gray500,
                shape = shape,
            ).clickableYGNoRipple(
                onClick = onClick,
                interactionSource = interactionSource,
            ).semantics { role = Role.Button },
        horizontalArrangement = Arrangement.spacedBy(
            space = YGTheme.layout.gap.gap1,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray700,
            textAlign = TextAlign.Center,
        )
        iconResource?.let {
            Image(
                painter = painterResource(id = it),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = YGAtomicColors.Gray.Gray700),
                modifier = Modifier.size(SizeTokens.Size20.getDp()),
            )
        }
    }
}

@YGPreview
@Composable
private fun YGMenuItemPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3)) {
        YGMenuItem(
            text = "카메라로 촬영",
            onClick = {},
        )
        YGMenuItem(
            text = "갤러리에서 선택",
            onClick = {},
        )
        YGMenuItem(
            text = "오늘의 파르페 가기",
            iconResource = R.drawable.ic_caret_right,
            onClick = {},
        )
    }
}
