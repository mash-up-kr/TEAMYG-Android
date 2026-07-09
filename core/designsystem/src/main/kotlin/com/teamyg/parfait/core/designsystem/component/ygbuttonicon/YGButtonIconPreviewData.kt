package com.teamyg.parfait.core.designsystem.component.ygbuttonicon

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

data class YGButtonIconPreviewData(
    val buttonIconSize: YGButtonIconSize,
    val isPressed: Boolean,
)

class YGButtonIconPreviewParameterProvider : PreviewParameterProvider<YGButtonIconPreviewData> {
    override val values = sequenceOf(
        YGButtonIconPreviewData(
            buttonIconSize = YGButtonIconSize.SIZE_44,
            isPressed = true,
        ),
        YGButtonIconPreviewData(
            buttonIconSize = YGButtonIconSize.SIZE_44,
            isPressed = false,
        ),
        YGButtonIconPreviewData(
            buttonIconSize = YGButtonIconSize.SIZE_48,
            isPressed = true,
        ),
        YGButtonIconPreviewData(
            buttonIconSize = YGButtonIconSize.SIZE_48,
            isPressed = false,
        ),
    )
}
