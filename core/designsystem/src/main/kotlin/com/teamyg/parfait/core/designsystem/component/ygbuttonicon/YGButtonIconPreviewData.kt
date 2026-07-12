package com.teamyg.parfait.core.designsystem.component.ygbuttonicon

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

data class YGButtonIconPreviewData(
    val buttonIconSize: YGButtonIconSize,
    val isEnabled: Boolean,
)

class YGButtonIconPreviewParameterProvider : PreviewParameterProvider<YGButtonIconPreviewData> {
    override val values = sequenceOf(
        YGButtonIconPreviewData(
            buttonIconSize = YGButtonIconSize.SIZE_44,
            isEnabled = true,
        ),
        YGButtonIconPreviewData(
            buttonIconSize = YGButtonIconSize.SIZE_44,
            isEnabled = false,
        ),
        YGButtonIconPreviewData(
            buttonIconSize = YGButtonIconSize.SIZE_48,
            isEnabled = true,
        ),
        YGButtonIconPreviewData(
            buttonIconSize = YGButtonIconSize.SIZE_48,
            isEnabled = false,
        ),
    )
}
