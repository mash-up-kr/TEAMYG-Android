package com.teamyg.parfait.core.designsystem.component.yginputnumber

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

data class YGInputNumberPreviewData(
    val isSelected: Boolean,
    val isPressed: Boolean,
)

class YGInputNumberPreviewParameterProvider : PreviewParameterProvider<YGInputNumberPreviewData> {
    override val values = sequenceOf(
        YGInputNumberPreviewData(false, false),
        YGInputNumberPreviewData(false, true),
        YGInputNumberPreviewData(true, false),
    )
}
