package com.teamyg.parfait.data.service.model.request.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * items·textItems 가 둘 다 비면 그룹·파르페 검사도 없이 200 + 빈 목록이다. 항목 하나라도 걸리면 이미지와
 * 텍스트가 함께 롤백되고, 어느 항목이 걸렸는지는 응답에 없다. 개수 상한은 없다(`docs/api/parfait-image.md`).
 */
@Serializable
data class UpdateParfaitImagesRequest(
    @SerialName("items")
    val items: List<UpdateParfaitImageItemRequest> = emptyList(),
    @SerialName("textItems")
    val textItems: List<UpdateParfaitTextItemRequest> = emptyList(),
)
