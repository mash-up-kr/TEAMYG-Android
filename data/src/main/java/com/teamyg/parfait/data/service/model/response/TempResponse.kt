package com.teamyg.parfait.data.service.model.response

import kotlinx.serialization.Serializable

// TODO 실제 API 확정 시 삭제 (구조 예시용)
@Serializable
data class TempResponse(
    val id: String,
    val name: String,
)
