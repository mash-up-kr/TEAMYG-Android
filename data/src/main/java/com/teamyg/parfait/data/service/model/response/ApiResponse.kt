package com.teamyg.parfait.data.service.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 로그아웃·회원 탈퇴·기기 토큰 등록은 204 본문 없음이라 이 envelope 이 오지 않는다(`docs/api/conventions.md`).
 *
 * @param code 성공은 "OK"·"CREATED" 두 값이라 성공 판정은 success 로 한다. 실패 코드 문자열은 도메인 enum
 *   사이에 유일하지 않으므로(MEMBER_NOT_FOUND 가 401·404 둘 다) HTTP status 와 함께 판정한다.
 * @param data 실패 시 null 이다.
 * @param errorDetail 서버가 현재 항상 null 로 보낸다. 검증 실패도 필드별 상세 없이 INVALID_REQUEST 하나다.
 */
@Serializable
data class ApiResponse<T>(
    @SerialName("success")
    val success: Boolean,
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val data: T? = null,
    @SerialName("errorDetail")
    val errorDetail: Map<String, String>? = null,
)
