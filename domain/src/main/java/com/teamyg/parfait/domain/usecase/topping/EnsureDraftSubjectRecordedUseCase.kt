package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 초안이 이 알맹이를 가리키게 맞춘다.
 *
 * 판정 기준은 "초안이 비었는가"가 아니라 "이 알맹이를 가리키는가"다
 * (`specs/archive/2026-08-20-c106-topping-place-api.md`).
 */
class EnsureDraftSubjectRecordedUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    /** @return 초안이 이 알맹이를 가리키게 되었으면 `true`. 이미 가리키던 경우도 포함한다 */
    suspend operator fun invoke(subjectImagePath: String): Boolean {
        val draftSubjectPath = toppingDraftRepository.draft.first()?.subjectImagePath
        if (draftSubjectPath == subjectImagePath) return true

        return toppingDraftRepository.record(
            subjectImagePath = subjectImagePath,
            cutoutImagePath = null,
            borderColorArgb = null,
            borderWidthDp = null,
        )
    }
}
