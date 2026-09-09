package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import javax.inject.Inject

/** 흐름이 만들어 낸 알맹이·테두리를 초안에 적는다 */
class RecordToppingDraftUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    /** @return 흐름이 열려 있지 않으면 `false`. 호출부가 이 값으로 실패를 알린다 */
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
