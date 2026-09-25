package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param years 오름차순이다. 보장 주체는 서비스가 아니라 서버 쿼리의 ORDER BY 다(`docs/api/parfait.md`).
 */
@Serializable
data class ParfaitYearsResponse(
    @SerialName("years")
    val years: List<Int>,
)
