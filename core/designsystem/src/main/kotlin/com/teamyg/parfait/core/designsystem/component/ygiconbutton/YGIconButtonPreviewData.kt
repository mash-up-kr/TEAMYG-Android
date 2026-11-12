package com.teamyg.parfait.core.designsystem.component.ygiconbutton

import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider

data class YGIconButtonPreviewData(
    val buttonIconSize: YGIconButtonSize,
    val isEnabled: Boolean,
)

class YGIconButtonPreviewParameterProvider : NamedPreviewParameterProvider<YGIconButtonPreviewData>() {
    override val entries = listOf(
        "size44" to YGIconButtonPreviewData(
            buttonIconSize = YGIconButtonSize.SIZE_44,
            isEnabled = true,
        ),
        "size44Disabled" to YGIconButtonPreviewData(
            buttonIconSize = YGIconButtonSize.SIZE_44,
            isEnabled = false,
        ),
        "size48" to YGIconButtonPreviewData(
            buttonIconSize = YGIconButtonSize.SIZE_48,
            isEnabled = true,
        ),
        "size48Disabled" to YGIconButtonPreviewData(
            buttonIconSize = YGIconButtonSize.SIZE_48,
            isEnabled = false,
        ),
    )
}
