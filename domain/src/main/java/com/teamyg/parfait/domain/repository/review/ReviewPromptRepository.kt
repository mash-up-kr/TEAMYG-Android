package com.teamyg.parfait.domain.repository.review

/**
 * 인앱 리뷰 유도 조건(업로드 3회 이상 + 로그인 유지 3일 이상)을 기기 단위로 추적한다.
 *
 * 호출 시도 자체를 4개월에 한 번만으로 조절한다. [checkAndMarkTriggered] 가 조건을 확인하고
 * true 면 같은 호출 안에서 다음 4개월을 잠근 뒤 돌려준다 — 호출부는 그 결과를 보고 네이티브
 * 리뷰 플로우를 띄우기만 하면 된다.
 */
interface ReviewPromptRepository {
    /** 로그인 상태를 처음 감지한 시점. 이미 있으면 아무것도 하지 않는다 */
    suspend fun markFirstSeenIfAbsent()

    suspend fun incrementUploadCount()

    /** 업로드 3회 이상 + 최초 감지 시점으로부터 3일 이상 + (미호출 또는 마지막 호출로부터 4개월 이상) */
    suspend fun checkAndMarkTriggered(): Boolean
}
