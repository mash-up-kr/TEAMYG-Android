package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase

internal suspend fun RecordToppingDraftUseCase.recordEditResult(result: ToppingEditResult): Boolean = this(
    subjectImagePath = result.subjectImagePath,
    cutoutImagePath = result.cutoutImagePath,
    sourceLongSide = result.sourceLongSide,
)
