package com.teamyg.parfait.domain.model.canvas

import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import kotlinx.datetime.LocalDateTime

/**
 * 캔버스 조회가 돌려주는 배치 토핑.
 *
 * 배치 확정 응답(PlacedToppingVO)과 필드 집합이 다르다 — 이쪽에만 테두리와 생성시각이 있다.
 * 두 타입을 합치면 POST 응답에 없는 값을 지어내거나 nullable 로 "모른다"와 "없다"를 뭉갠다.
 * 공통 조각(ToppingTransform·ToppingBorder·ToppingPlacerVO)은 그대로 재사용한다.
 *
 * placedBy 의 groupMemberId 가 같은 응답의 members 에 없을 수 있다 — 탈퇴·이탈한 멤버의
 * 토핑은 남고 닉네임이 "(알수없음)"으로 온다(`api/parfait.md`).
 *
 * @param isMine 서버가 판정해 준 값. 판정 축이 계정 id 라 [placedBy] 의 groupMemberId 로는
 *   재현할 수 없다.
 */
data class CanvasToppingVO(
    val parfaitImageId: ParfaitImageId,
    val imageId: ImageId,
    val imageUrl: String,
    val transform: ToppingTransform,
    val border: ToppingBorder,
    val placedBy: ToppingPlacerVO,
    val isMine: Boolean,
    val createdAt: LocalDateTime,
)
