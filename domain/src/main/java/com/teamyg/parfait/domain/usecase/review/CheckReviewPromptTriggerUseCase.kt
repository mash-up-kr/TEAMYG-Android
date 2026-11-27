package com.teamyg.parfait.domain.usecase.review

import com.teamyg.parfait.domain.repository.review.ReviewPromptRepository
import javax.inject.Inject

/**
 * 업로드(배경·텍스트·사진 토핑, 종류 무관) 3회 이상 + 로그인 유지 3일 이상이면 Google Play
 * 인앱 리뷰를 요청할 차례라고 알린다. 기기 단위로 추적한다.
 */
class CheckReviewPromptTriggerUseCase @Inject constructor(
    private val reviewPromptRepository: ReviewPromptRepository,
) {
    suspend operator fun invoke(): Boolean = reviewPromptRepository.checkTrigger()
}
