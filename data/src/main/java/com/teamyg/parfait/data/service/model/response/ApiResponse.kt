package com.teamyg.parfait.data.service.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: String,
    val message: String,
    val data: T? = null,
) {
    val isSuccess: Boolean get() = code == SUCCESS_CODE

    companion object {
        // TODO 실제 백엔드 성공 코드로 교체
        private const val SUCCESS_CODE = "SUCCESS"
    }
}
