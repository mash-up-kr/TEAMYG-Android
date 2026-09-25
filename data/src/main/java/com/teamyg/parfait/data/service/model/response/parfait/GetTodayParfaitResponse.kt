package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * images 는 배치가 0건이면 빈 배열이 아니라 null 이다. background 도 type·value 중 하나라도
 * 없으면 통째로 null 이다. 서버가 default-property-inclusion: always 라 키 자체는 실려 오므로
 * 키 존재가 아니라 값이 null 인지로 갈라야 한다(`docs/api/parfait.md`).
 *
 * 상세 조회(`/parfaits/{parfaitId}`)도 이 응답을 그대로 쓴다.
 *
 * @param date 서버의 하루는 자정이 아니라 03:00 에 넘어간다(`ParfaitDay`).
 * @param status 오늘 조회여도 ACTIVE 가 아닐 수 있다(서버가 날짜로 찾는다). 마감된 캔버스에 쓰면
 *   409 PARFAIT_ALREADY_CLOSED 다.
 * @param lastClosedDate 조회 대상이 아니라 그룹 기준 마지막 CLOSED 날짜다. EMPTY 로 마감된 날은 세지 않고,
 *   상세 조회에서는 대상 날짜보다 뒤일 수 있다.
 * @param groupMembers 탈퇴하지 않은 멤버만 참여 순으로 온다. 탈퇴자가 남긴 토핑의 `placedBy.groupMemberId`
 *   는 여기 없을 수 있다.
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
