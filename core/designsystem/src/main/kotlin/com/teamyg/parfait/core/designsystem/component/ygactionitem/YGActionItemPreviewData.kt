package com.teamyg.parfait.core.designsystem.component.ygactionitem

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

data class YGActionItemPreviewData(
    val isPressed: Boolean,
)

class YGActionItemPreviewParameterProvider : PreviewParameterProvider<YGActionItemPreviewData> {
    override val values = sequenceOf(
        YGActionItemPreviewData(
            isPressed = false,
        ),
        YGActionItemPreviewData(
            isPressed = true,
        ),
    )
}
