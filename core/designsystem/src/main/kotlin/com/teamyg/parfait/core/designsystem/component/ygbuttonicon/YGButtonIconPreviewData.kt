package com.teamyg.parfait.core.designsystem.component.ygbuttonicon

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

data class YGButtonIconPreviewData(
    val buttonIconSize: YGButtonIconSize,
)

class YGButtonIconPreviewParameterProvider : PreviewParameterProvider<YGButtonIconPreviewData> {
    override val values = sequenceOf(
        YGButtonIconPreviewData(
            buttonIconSize = YGButtonIconSize.SIZE_44,
        ),
        YGButtonIconPreviewData(
            buttonIconSize = YGButtonIconSize.SIZE_48,
        ),
    )
}
