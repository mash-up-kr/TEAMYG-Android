package com.teamyg.parfait.core.designsystem.component.ygiconbutton

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

data class YGIconButtonPreviewData(
    val buttonIconSize: YGIconButtonSize,
    val isEnabled: Boolean,
)

class YGIconButtonPreviewParameterProvider : PreviewParameterProvider<YGIconButtonPreviewData> {
    override val values = sequenceOf(
        YGIconButtonPreviewData(
            buttonIconSize = YGIconButtonSize.SIZE_44,
            isEnabled = true,
        ),
        YGIconButtonPreviewData(
            buttonIconSize = YGIconButtonSize.SIZE_44,
            isEnabled = false,
        ),
        YGIconButtonPreviewData(
            buttonIconSize = YGIconButtonSize.SIZE_48,
            isEnabled = true,
        ),
        YGIconButtonPreviewData(
            buttonIconSize = YGIconButtonSize.SIZE_48,
            isEnabled = false,
        ),
    )
}
