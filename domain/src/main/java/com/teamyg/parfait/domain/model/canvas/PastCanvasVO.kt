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
    val status: CanvasStatus,
    val thumbnailUrl: String?,
    val toppingCount: Int,
) {
    /**
     * 캔버스만 열어 보고 토핑은 안 올린 날. 달력이 점을 찍으면 안 되는 날이다.
     *
     * [status] 의 EMPTY 와 뜻이 다르니 갈아타지 말 것 — 그쪽은 "0건으로 마감된 날"이라
     * 아직 진행 중인 오늘 캔버스가 빠진다. 점 기준을 토핑 개수로 두는 것은 C-201 정책이다.
     */
    val isEmpty: Boolean get() = toppingCount == 0
}
