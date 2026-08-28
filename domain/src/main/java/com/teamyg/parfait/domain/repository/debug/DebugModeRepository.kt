package com.teamyg.parfait.domain.repository.debug

import kotlinx.coroutines.flow.Flow

/**
 * 개발·QA 편의를 위한 디버그 모드 플래그. 저장소가 단일 진실이고 화면 상태는 그 투영이다.
 *
 * 정책은 `specs/2026-08-28-login-debug-mode.md` 가 정본이다.
 */
interface DebugModeRepository {
    val isEnabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)
}
