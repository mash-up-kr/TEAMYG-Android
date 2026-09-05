package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import javax.inject.Inject

/** 이름보다 넓게 쓴다 — 편집 결과에 한정되지 않는다 */
class SaveEditedImageUseCase
@Inject
constructor(
    private val repository: ImageSegmentationRepository,
) {
    init {
        useCaseLogger.i { "SaveEditedImageUseCase::init" }
    }

    suspend operator fun invoke(bitmapWrapper: BitmapWrapper): Result<String> =
        repository.saveEditedImage(bitmapWrapper)
}
