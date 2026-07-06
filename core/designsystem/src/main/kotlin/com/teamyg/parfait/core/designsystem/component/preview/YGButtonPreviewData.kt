package com.teamyg.parfait.core.designsystem.component.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.YGButtonType

data class YGButtonPreviewData(
    val name: String,
    val buttonType: YGButtonType,
)

class YGButtonPreviewParameterProvider : PreviewParameterProvider<YGButtonPreviewData> {
    override val values = sequenceOf(
        YGButtonPreviewData(
            name = "XSmall",
            buttonType = YGButtonType.XSmall,
        ),
        YGButtonPreviewData(
            name = "Small",
            buttonType = YGButtonType.Small,
        ),
        YGButtonPreviewData(
            name = "SmallSquare",
            buttonType = YGButtonType.SmallSquare,
        ),
        YGButtonPreviewData(
            name = "Medium Primary",
            buttonType = YGButtonType.Medium.Primary,
        ),
        YGButtonPreviewData(
            name = "Medium Secondary",
            buttonType = YGButtonType.Medium.Secondary,
        ),
        YGButtonPreviewData(
            name = "Medium Transparency",
            buttonType = YGButtonType.Medium.Transparency,
        ),
        YGButtonPreviewData(
            name = "Large",
            buttonType = YGButtonType.Large,
        ),
    )
}
