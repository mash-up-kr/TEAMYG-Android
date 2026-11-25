package com.teamyg.parfait.domain.usecase.review

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.review.ReviewPromptRepository
import javax.inject.Inject

/**
 * 인앱 리뷰 유도 조건(업로드 3회 이상)에 쓸 업로드 횟수를 기기 단위로 센다. 배경·텍스트·사진
 * 토핑, 종류 무관하게 업로드 성공 경로에서 그대로 부른다.
 */
class RecordReviewPromptUploadUseCase @Inject constructor(
    private val reviewPromptRepository: ReviewPromptRepository,
) {
    suspend operator fun invoke() {
        runSuspendCatching { reviewPromptRepository.incrementUploadCount() }
            .onFailure { useCaseLogger.e(it) { "RecordReviewPromptUploadUseCase - 업로드 집계 실패" } }
    }
}
