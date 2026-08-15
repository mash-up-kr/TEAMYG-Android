package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitImageId

/**
 * 테두리 수정 결과.
 *
 * 앱이 서버로부터 테두리를 되받는 첫 자리다 — 배치 확정·위치 수정 두 응답은 테두리를
 * 저장만 하고 돌려주지 않는다(`api/parfait-image.md`).
 */
data class UpdatedToppingBorderVO(
    val parfaitImageId: ParfaitImageId,
    val border: ToppingBorder,
)
