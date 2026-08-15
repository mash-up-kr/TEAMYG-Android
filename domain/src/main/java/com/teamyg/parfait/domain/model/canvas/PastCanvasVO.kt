package com.teamyg.parfait.domain.model.canvas

import com.teamyg.parfait.domain.model.id.ParfaitId
import kotlinx.datetime.LocalDate

/**
 * 과거 캔버스 목록의 한 칸.
 *
 * thumbnailUrl 은 서버가 항상 null 을 넣는다 — 필드만 있고 채우는 코드가 없다
 * (`api/parfait.md`). 빼지도 지어내지도 않고 그대로 노출한다.
 *
 * 서버 응답 필드명은 imageCount 인데 domain 은 제품 언어를 쓰므로 toppingCount 다.
 */
data class PastCanvasVO(
    val parfaitId: ParfaitId,
    val date: LocalDate,
    val thumbnailUrl: String?,
    val toppingCount: Int,
)
