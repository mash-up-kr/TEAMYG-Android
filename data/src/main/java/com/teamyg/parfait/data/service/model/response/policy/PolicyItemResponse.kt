package com.teamyg.parfait.data.service.model.response.policy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param type 서버가 enum 이름 문자열로 준다(`TERMS_OF_SERVICE`·`PRIVACY_POLICY`).
 * @param url 서버가 URL 전용 컬럼이 아니라 약관 본문 컬럼을 그대로 매핑한 값이라
 *   링크가 아닐 수 있다(계약 문서 `api/policy.md` 미결).
 */
@Serializable
data class PolicyItemResponse(
    @SerialName("termsId")
    val termsId: Long,
    @SerialName("type")
    val type: String,
    @SerialName("title")
    val title: String,
    @SerialName("url")
    val url: String,
    @SerialName("required")
    val required: Boolean,
)
