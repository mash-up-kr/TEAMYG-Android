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
 * @param nameTagChip 서버가 그 그룹 안에서 배정한 칩. 이 목록은 탈퇴자를 빼고 오므로 `"DEFAULT"` 는
 *  오지 않는다.
 */
@Serializable
data class GroupMemberResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("nameTagChip")
    val nameTagChip: String? = null,
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
 * 배치자. `response/parfaitimage` 의 `PlaceParfaitImagePlacedByResponse` 와 이름이 다른 것은
 * 서버가 그렇기 때문이다(사유는 `api/parfait-image.md`) — 통일하려 들지 말 것.
 *
 * @param nickname 그룹 닉네임이다. 탈퇴·이탈한 멤버면 "(알수없음)"이 온다.
 * @param nameTagChip 그 사람의 칩. 탈퇴했으면 `"DEFAULT"` 다. 읽는 화면이 생길 때 도메인으로
 *  올린다 — 소비자 없이 [com.teamyg.parfait.domain.model.topping.ToppingPlacerVO] 모양을 굳히지 않는다.
 *  ⚠️ 칩이 필요한 화면이라고 해서 자동으로 이 값은 아니다. 상단 멤버 칩을 그리는 자리는
 *  `groupMembers` 를 `GroupMemberId` 로 조인해 찾는다.
 * @param ownerType 서버가 요청자 기준으로 판정한 소유(`"ME"`·`"OTHER"`). 판정 축이 계정 id 라
 *  [groupMemberId] 로는 재현할 수 없다 — 소유를 가릴 때 그 값을 견주지 말 것.
 */
@Serializable
data class PlacedByResponse(
    @SerialName("groupMemberId")
    val groupMemberId: Long,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("nameTagChip")
    val nameTagChip: String? = null,
    @SerialName("ownerType")
    val ownerType: String? = null,
)
