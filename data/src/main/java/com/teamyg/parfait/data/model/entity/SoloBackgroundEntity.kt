package com.teamyg.parfait.data.model.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SoloBackgroundEntity {
    @Serializable
    @SerialName("color")
    data class Color(val hex: String) : SoloBackgroundEntity

    /** 절대경로가 아니라 `images/` 안의 파일명이다 — 읽을 때 디렉터리를 붙인다 */
    @Serializable
    @SerialName("image")
    data class Image(val fileName: String) : SoloBackgroundEntity
}
