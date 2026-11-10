package com.teamyg.parfait.data.service.model.response.notice

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param type 서버 enum 이름 문자열(`UPDATE`·`GUIDE`). 화면이 구분하지 않아 매핑하지 않는다.
 * @param publishedDate `yyyy-MM-dd` (KST 기준 게시일)
 */
@Serializable
data class NoticeResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("type")
    val type: String,
    @SerialName("title")
    val title: String,
    @SerialName("content")
    val content: String,
    @SerialName("publishedDate")
    val publishedDate: String,
)
