package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 캔버스 배경 변경 응답.
 *
 * 중첩 [BackgroundResponse] 는 조회 응답의 것을 그대로 쓴다 — 서버도 같은 클래스를 재사용한다.
 * 조회에서는 background 가 널 허용이지만 여기서는 방금 설정한 값이라 비널이다.
 *
 * type 이 IMAGE 면 value 는 imageId 가 아니라 저장된 이미지 URL 이다 — 요청은 id 로 보내고
 * 응답은 URL 로 받는다(`api/parfait.md`).
 */
@Serializable
data class ChangeParfaitBackgroundResponse(
    @SerialName("background")
    val background: BackgroundResponse,
)
