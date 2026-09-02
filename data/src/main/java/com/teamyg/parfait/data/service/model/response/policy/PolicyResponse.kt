package com.teamyg.parfait.data.service.model.response.policy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PolicyResponse(
    @SerialName("policies")
    val policies: List<PolicyItemResponse>,
)
