package com.teamyg.parfait.domain.repository.review

/**
 * 인앱 리뷰 유도 조건(업로드 3회 이상 + 로그인 유지 3일 이상)을 기기 단위로 추적한다.
 *
 * 호출 시도 자체를 4개월에 한 번만으로 조절한다. [checkTrigger] 가 조건을 확인하고 true 면
 * 같은 호출 안에서 다음 4개월을 잠근 뒤 돌려준다 — 호출부는 그 결과를 보고 네이티브 리뷰
 * 플로우를 띄우기만 하면 된다.
 */
interface ReviewPromptRepository {
    suspend fun incrementUploadCount()

    /** 최초 감지 기록(없으면 세움) + 업로드 3회 이상 + 유지 3일 이상 + 재시도 4개월 경과를 한 번에 본다 */
    suspend fun checkTrigger(): Boolean
}
