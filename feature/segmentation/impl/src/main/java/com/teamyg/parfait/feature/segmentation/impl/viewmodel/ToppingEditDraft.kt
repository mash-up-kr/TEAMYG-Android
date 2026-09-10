package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import com.teamyg.parfait.domain.model.image.SourceLongSide
import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult

internal suspend fun RecordToppingDraftUseCase.recordEditResult(result: ToppingEditResult): Boolean {
    val border = result.borderLayers.lastOrNull()

    return this(
        subjectImagePath = result.subjectImagePath,
        cutoutImagePath = result.cutoutImagePath,
        borderColorArgb = border?.colorArgb,
        borderWidthDp = border?.widthDp,
        sourceLongSide = result.sourceLongSide?.let(::SourceLongSide),
    )
}
