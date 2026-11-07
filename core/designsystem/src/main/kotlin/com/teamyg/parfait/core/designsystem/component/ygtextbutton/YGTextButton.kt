package com.teamyg.parfait.core.designsystem.component.ygtextbutton

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple

/**
 * Figma Text-Button. Caption/C01_R(12sp), 기본 Gray400 · pressed Gray500.
 */
@Composable
fun YGTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val color = if (isPressed) YGAtomicColors.Gray.Gray500 else YGAtomicColors.Gray.Gray400

    Text(
        text = text,
        color = color,
        style = YGTheme.typography.caption.c01R,
        modifier = modifier
            .clickableYGNoRipple(
                interactionSource = interactionSource,
                onClick = onClick,
            ).semantics { role = Role.Button },
    )
}

@YGPreview
@Composable
private fun YGTextButtonPreview() = PreviewBox {
    Row(horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3)) {
        YGTextButton(text = "이용약관", onClick = {})
        YGTextButton(text = "개인정보처리방침", onClick = {})
    }
}
