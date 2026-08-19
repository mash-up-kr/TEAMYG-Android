package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 오늘의 캔버스 조회 응답.
 *
 * images 는 배치가 0건이면 빈 배열이 아니라 null 이다. background 도 type·value 중 하나라도
 * 없으면 통째로 null 이다. 서버가 default-property-inclusion: always 라 키 자체는 실려 오므로
 * 키 존재가 아니라 값이 null 인지로 갈라야 한다(`api/parfait.md`).
 */
@Serializable
data class GetTodayParfaitResponse(
    @SerialName("parfaitId")
    val parfaitId: Long,
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

/**
 * @param id 계정 id 가 아니라 그룹 멤버십 행 id 다.
 */
@Serializable
data class GroupMemberResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("nickname")
    val nickname: String,
)

/**
 * @param value type 이 COLOR 면 색 문자열, IMAGE 면 URL 이다.
 */
@Serializable
data class BackgroundResponse(
    @SerialName("type")
    val type: String,
    @SerialName("value")
    val value: String,
)

@Serializable
data class TodayParfaitImageResponse(
    @SerialName("parfaitImageId")
    val parfaitImageId: Long,
    @SerialName("imageId")
    val imageId: Long,
    @SerialName("imageUrl")
    val imageUrl: String,
    @SerialName("positionX")
    val positionX: Double,
    @SerialName("positionY")
    val positionY: Double,
    @SerialName("positionZ")
    val positionZ: Int,
    @SerialName("scale")
    val scale: Double,
    @SerialName("rotation")
    val rotation: Double,
    @SerialName("borderType")
    val borderType: String,
    @SerialName("borderColor")
    val borderColor: String? = null,
    @SerialName("borderWidth")
    val borderWidth: Double? = null,
    @SerialName("placedBy")
    val placedBy: PlacedByResponse,
    @SerialName("createdAt")
    val createdAt: String,
)

/**
 * 배치자. 같은 이름의 DTO 가 response/parfaitimage 에도 있었으나 서버가 그쪽을
 * PlaceParfaitImagePlacedByResponse 로 개명했다(springdoc 이 두 스키마를 같은 것으로 취급해
 * 이쪽에 추가한 칩이 스웨거에 안 보이던 문제 때문이다). 이 클래스는 서버가 이름을 안 바꿨다.
 *
 * @param nickname 그룹 닉네임이다. 탈퇴·이탈한 멤버면 "(알수없음)"이 온다.
 * @param nameTagChip 그 사람의 칩. 탈퇴했으면 `"DEFAULT"` 다. **아직 도메인으로 올리지 않는다** —
 *  서버는 이제 배치 확정 응답에도 이 값을 주므로 [com.teamyg.parfait.domain.model.topping.ToppingPlacerVO]
 *  를 채울 수 있게 됐지만, `placedBy` 를 읽는 화면이 0건이다. C-202 Spotlight 는 이 값이 아니라
 *  groupMembers 를 GroupMemberId 로 조인해 찾으므로 이 보류에 물리지 않는다.
 */
@Serializable
data class PlacedByResponse(
    @SerialName("groupMemberId")
    val groupMemberId: Long,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("nameTagChip")
    val nameTagChip: String? = null,
)
