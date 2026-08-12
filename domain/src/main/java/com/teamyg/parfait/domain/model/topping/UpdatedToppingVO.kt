package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitImageId

/**
 * 수정 결과. 배치 응답(PlacedToppingVO)과 달리 imageId·imageUrl·placedBy 가 없다 —
 * 같은 리소스인데 서버의 두 응답 필드 집합이 다르다(`api/parfait-image.md`).
 */
data class UpdatedToppingVO(
    val parfaitImageId: ParfaitImageId,
    val transform: ToppingTransform,
)
