package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 오늘의 캔버스 조회 응답.
 *
 * images 는 배치가 0건이면 빈 배열이 아니라 null 이다. background 도 type·value 중 하나라도
 * 없으면 통째로 null 이다. 서버가 default-property-inclusion: always 라 키 자체는 실려 오므로
 * 키 존재가 아니라 값이 null 인지로 갈라야 한다.
 */
@Serializable
data class GetTodayParfaitResponse(
    @SerialName("parfaitId")
    val parfaitId: Long,
    @SerialName("groupName")
    val groupName: String,
    @SerialName("date")
    val date: String,
    @SerialName("status")
    val status: String,
    @SerialName("lastClosedDate")
    val lastClosedDate: String? = null,
    @SerialName("groupMembers")
    val groupMembers: List<GroupMemberResponse>,
    @SerialName("background")
    val background: BackgroundResponse? = null,
    @SerialName("images")
    val images: List<TodayParfaitImageResponse>? = null,
)
