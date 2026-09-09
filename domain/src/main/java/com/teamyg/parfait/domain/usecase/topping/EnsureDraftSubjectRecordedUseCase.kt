package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 판정 기준은 "초안이 비었는가"가 아니라 "이 알맹이를 가리키는가"다
 * (`specs/archive/2026-08-20-c106-topping-place-api.md`).
 */
class EnsureDraftSubjectRecordedUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    /** @return 이미 가리키던 경우도 `true` 다 */
    suspend operator fun invoke(subjectImagePath: String): Boolean {
        val draftSubjectPath = toppingDraftRepository.draft.first()?.subjectImagePath
        if (draftSubjectPath == subjectImagePath) return true

        return toppingDraftRepository.record(
            subjectImagePath = subjectImagePath,
            cutoutImagePath = null,
            borderColorArgb = null,
            borderWidthDp = null,
            // 최근 목록에서 되살린 알맹이는 세그멘테이션을 타지 않아 오려낸 사진의
            // 치수를 알 방법이 없다. 배율을 지어내지 않고 방어선에만 맡긴다
            sourceLongSide = null,
        )
    }
}
