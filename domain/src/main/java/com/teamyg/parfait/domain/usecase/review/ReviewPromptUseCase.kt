package com.teamyg.parfait.domain.usecase.review

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.review.ReviewPromptRepository
import javax.inject.Inject

/**
 * 업로드(배경·텍스트·사진 토핑, 종류 무관) 3회 이상 + 로그인 유지 3일 이상이면 Google Play
 * 인앱 리뷰를 요청한다. 기기 단위로 추적한다 — 로그아웃·계정 전환과 무관하다.
 */
class ReviewPromptUseCase @Inject constructor(
    private val reviewPromptRepository: ReviewPromptRepository,
) {
    /**
     * 업로드 성공 경로에서 그대로 부른다. 여기서 실패를 삼키는 이유: 서버 저장은 이미 끝난
     * 뒤라, 집계만 실패했다고 업로드 자체를 에러로 보이면 사용자가 재시도해 중복 업로드가
     * 생긴다.
     */
    suspend fun recordUpload() {
        runSuspendCatching { reviewPromptRepository.incrementUploadCount() }
            .onFailure { useCaseLogger.e(it) { "ReviewPromptUseCase - 업로드 집계 실패" } }
    }

    /** 조건이 맞으면 그 자리에서 다음 4개월을 잠그고 true 를 돌린다 */
    suspend fun checkTrigger(): Boolean = reviewPromptRepository.checkTrigger()
}
