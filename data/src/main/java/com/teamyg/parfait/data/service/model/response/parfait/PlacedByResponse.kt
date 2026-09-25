package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 배치자. `response/parfaitimage` 의 `PlaceParfaitImagePlacedByResponse` 와 이름이 다른 것은 서버 이름을
 * 따랐기 때문이다(`docs/api/parfait-image.md`). 통일하거나 이름을 바꾸지 않는다.
 *
 * @param nickname 그룹 닉네임이다. 탈퇴·이탈한 멤버면 "(알수없음)"이 온다.
 * @param nameTagChip 그 사람의 칩. 탈퇴했으면 `"DEFAULT"` 다. 읽는 화면이 생길 때 도메인으로 올린다 —
 *  소비자 없이 [com.teamyg.parfait.domain.model.topping.ToppingPlacerVO] 모양을 굳히지 않는다.
 *  상단 멤버 칩은 이 값이 아니라 `groupMembers` 를 `GroupMemberId` 로 조인해 찾는다.
 * @param ownerType 서버가 요청자 기준으로 판정한 소유(`"ME"`·`"OTHER"`). 판정 축이 계정 id 라
 *  [groupMemberId] 로는 재현할 수 없다 — 소유를 가릴 때 그 값을 견주지 말 것. 같은 캔버스라도 보는 사람마다
 *  값이 달라 응답을 사용자 사이에 돌려 쓰면 안 된다(`docs/api/parfait.md`).
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
