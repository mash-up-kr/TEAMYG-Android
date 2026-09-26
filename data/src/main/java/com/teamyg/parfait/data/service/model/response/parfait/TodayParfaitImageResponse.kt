package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
