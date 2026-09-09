package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import javax.inject.Inject

class RecordToppingDraftUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    /** @return 흐름이 열려 있지 않으면 `false`. 없는 초안을 지어내지 않는다 */
    suspend operator fun invoke(
        subjectImagePath: String,
        cutoutImagePath: String?,
        borderColorArgb: Int?,
        borderWidthDp: Float?,
    ): Boolean = toppingDraftRepository.record(
        subjectImagePath = subjectImagePath,
        cutoutImagePath = cutoutImagePath,
        borderColorArgb = borderColorArgb,
        borderWidthDp = borderWidthDp,
    )
}
