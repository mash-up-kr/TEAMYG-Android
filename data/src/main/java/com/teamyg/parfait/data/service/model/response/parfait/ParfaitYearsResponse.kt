package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParfaitYearsResponse(
    @SerialName("years")
    val years: List<Int>,
)
