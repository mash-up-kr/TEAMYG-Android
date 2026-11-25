package com.teamyg.parfait.domain.usecase.review

import com.teamyg.parfait.domain.repository.review.ReviewPromptRepository
import javax.inject.Inject

/**
 * 업로드(배경·텍스트·사진 토핑, 종류 무관) 3회 이상 + 로그인 유지 3일 이상이면 Google Play
 * 인앱 리뷰를 요청한다. 기기 단위로 추적한다 — 로그아웃·계정 전환과 무관하다.
 */
class ReviewPromptUseCase @Inject constructor(
    private val reviewPromptRepository: ReviewPromptRepository,
) {
    suspend fun markFirstSeen() = reviewPromptRepository.markFirstSeenIfAbsent()

    suspend fun recordUpload() = reviewPromptRepository.incrementUploadCount()

    /** 조건이 맞으면 그 자리에서 다음 4개월을 잠그고 true 를 돌린다 */
    suspend fun checkAndMarkTriggered(): Boolean = reviewPromptRepository.checkAndMarkTriggered()
}
