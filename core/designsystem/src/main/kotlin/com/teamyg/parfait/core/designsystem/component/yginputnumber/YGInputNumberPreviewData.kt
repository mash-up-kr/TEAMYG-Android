package com.teamyg.parfait.core.designsystem.component.yginputnumber

import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider

data class YGInputNumberPreviewData(
    val isSelected: Boolean,
)

class YGInputNumberPreviewParameterProvider : NamedPreviewParameterProvider<YGInputNumberPreviewData>() {
    override val entries = listOf(
        "default" to YGInputNumberPreviewData(false),
        "selected" to YGInputNumberPreviewData(true),
    )
}
