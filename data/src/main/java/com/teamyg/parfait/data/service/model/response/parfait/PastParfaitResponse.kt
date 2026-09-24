package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param status 오늘 조회·상세와 같은 값 집합이고, EMPTY 는 imageCount == 0 과 뜻이 다르다.
 * @param thumbnailUrl 서버가 항상 null 을 넣는다.
 */
@Serializable
data class PastParfaitResponse(
    @SerialName("parfaitId")
    val parfaitId: Long,
    @SerialName("date")
    val date: String,
    @SerialName("status")
    val status: String,
    @SerialName("thumbnailUrl")
    val thumbnailUrl: String? = null,
    @SerialName("imageCount")
    val imageCount: Int,
)
