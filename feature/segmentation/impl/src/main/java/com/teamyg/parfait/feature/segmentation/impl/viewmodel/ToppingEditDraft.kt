package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase
import com.teamyg.parfait.feature.segmentation.impl.model.ToppingEditResult

internal suspend fun RecordToppingDraftUseCase.recordEditResult(result: ToppingEditResult): Boolean = this(
    subjectImagePath = result.subjectImagePath,
    cutoutImagePath = result.cutoutImagePath,
    sourceLongSide = result.sourceLongSide,
)
