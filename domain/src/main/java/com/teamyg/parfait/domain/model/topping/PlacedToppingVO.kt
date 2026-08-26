package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitImageId

/**
 * 배치 확정 결과.
 *
 * 테두리 필드가 없다. 서버가 저장은 하는데 응답에 돌려주지 않기 때문이다 — 없는 것을
 * 지어내지 않는다. 앱이 테두리 상태를 알려면 자기가 보낸 값을 기억해야 한다.
 */
data class PlacedToppingVO(
    val parfaitImageId: ParfaitImageId,
    val imageId: ImageId,
    val imageUrl: String,
    val transform: ToppingTransform,
    val placedBy: ToppingPlacerVO,
)

/**
 * @param nickname 전역 닉네임이 아니라 그룹 안에서 쓰는 이름이다.
 * @param isMine 이 토핑을 나 자신이 놓았는지 서버가 직접 알려주는 값이다.
 */
data class ToppingPlacerVO(
    val groupMemberId: GroupMemberId,
    val nickname: GroupNickname,
    val isMine: Boolean,
)
