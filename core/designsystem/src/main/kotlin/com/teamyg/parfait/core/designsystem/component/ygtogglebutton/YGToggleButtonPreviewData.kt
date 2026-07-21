package com.teamyg.parfait.core.designsystem.component.ygtogglebutton

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.R

data class YGToggleButtonPreviewData(
    val isSelected: Boolean,
    val iconResource: Int?,
)

class YGToggleButtonPreviewParameterProvider : PreviewParameterProvider<YGToggleButtonPreviewData> {
    override val values = sequenceOf(
        YGToggleButtonPreviewData(
            isSelected = true,
            iconResource = null,
        ),
        YGToggleButtonPreviewData(
            isSelected = false,
            iconResource = null,
        ),
        YGToggleButtonPreviewData(
            isSelected = true,
            iconResource = R.drawable.ic_plus,
        ),
        YGToggleButtonPreviewData(
            isSelected = false,
            iconResource = R.drawable.ic_plus,
        ),
    )
}
