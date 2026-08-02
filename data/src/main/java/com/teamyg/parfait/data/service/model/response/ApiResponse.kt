package com.teamyg.parfait.data.service.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val code: String,
    val message: String,
    val data: T? = null,
    val errorDetail: Map<String, String>? = null,
)
