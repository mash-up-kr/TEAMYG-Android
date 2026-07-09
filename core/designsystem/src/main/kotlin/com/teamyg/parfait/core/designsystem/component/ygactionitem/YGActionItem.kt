package com.teamyg.parfait.core.designsystem.component.ygactionitem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Composable
fun YGActionItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    YGActionItem(
        text = text,
        isPressed = isPressed,
        modifier = modifier
            .clickable(onClick = onClick, interactionSource = interactionSource)
            .semantics { role = Role.Button },
    )
}

@Composable
private fun YGActionItem(
    text: String,
    isPressed: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 16.dp),
    ) {
        Text(
            text = text,
            style = YGTheme.typography.body.b02R,
            color = if (isPressed) YGAtomicColors.Gray.Gray700 else YGAtomicColors.Gray.Gray500,
        )
    }
}

@Preview
@Composable
fun YGActionItemPreview(
    @PreviewParameter(YGActionItemPreviewParameterProvider::class)
    data: YGActionItemPreviewData,
) {
    YGCustomTheme {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            YGActionItem(
                text = "그룹 나가기",
                isPressed = data.isPressed,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White),
            )
        }
    }
}
