package com.teamyg.parfait.core.designsystem.component.ygbutton

import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider

data class YGButtonPreviewData(
    val name: String,
    val buttonType: YGButtonType,
)

class YGButtonPreviewParameterProvider : NamedPreviewParameterProvider<YGButtonPreviewData>() {
    override val entries = listOf(
        "smallSquare" to YGButtonPreviewData(
            name = "SmallSquare",
            buttonType = YGButtonType.SmallSquare,
        ),
        "mediumPrimary" to YGButtonPreviewData(
            name = "Medium Primary",
            buttonType = YGButtonType.Medium.Primary,
        ),
        "mediumSecondary" to YGButtonPreviewData(
            name = "Medium Secondary",
            buttonType = YGButtonType.Medium.Secondary,
        ),
        "mediumTransparency" to YGButtonPreviewData(
            name = "Medium Transparency",
            buttonType = YGButtonType.Medium.Transparency,
        ),
        "large" to YGButtonPreviewData(
            name = "Large",
            buttonType = YGButtonType.Large,
        ),
    )
}
