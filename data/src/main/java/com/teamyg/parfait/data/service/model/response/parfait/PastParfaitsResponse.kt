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

/**
 * @param status 오늘 조회·상세와 같은 값 집합이다. EMPTY 는 "비어 있음"이 아니라
 * "빈 채로 마감됨"이라 imageCount == 0 과 뜻이 다르다(`api/parfait.md`).
 * @param thumbnailUrl 서버가 항상 null 을 넣는다. 채우는 코드가 없다(`api/parfait.md`).
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
