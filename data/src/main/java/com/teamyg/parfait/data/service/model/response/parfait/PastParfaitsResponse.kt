package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 과거 캔버스 목록 응답. 0건이면 빈 배열이다 — today 의 images 가 null 인 것과 반대다.
 *
 * @param parfaits 날짜 내림차순이다. 상태로 거르지 않아 범위에 오늘이 들면 ACTIVE 캔버스도 포함된다
 *   (`docs/api/parfait.md`).
 */
@Serializable
data class PastParfaitsResponse(
    @SerialName("parfaits")
    val parfaits: List<PastParfaitResponse>,
)
