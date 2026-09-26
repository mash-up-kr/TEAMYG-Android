package com.teamyg.parfait.data.service.model.request.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param items 빈 배열이면 그룹·파르페 검사도 없이 200 + `images: []` 다. 항목 하나라도 404·403 이면
 *   전부 롤백되고, 어느 항목이 걸렸는지는 응답에 없다. 개수 상한은 없다(`docs/api/parfait-image.md`).
 */
@Serializable
data class UpdateParfaitImagesRequest(
    @SerialName("items")
    val items: List<UpdateParfaitImageItemRequest>,
)
