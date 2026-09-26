package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 과거 캔버스 목록 응답. 0건이면 빈 배열이다 — today 의 images 가 null 인 것과 반대다.
 */
@Serializable
data class PastParfaitsResponse(
    @SerialName("parfaits")
    val parfaits: List<PastParfaitResponse>,
)
