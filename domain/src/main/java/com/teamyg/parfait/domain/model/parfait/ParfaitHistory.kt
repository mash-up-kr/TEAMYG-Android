package com.teamyg.parfait.domain.model.parfait

import com.teamyg.parfait.domain.model.id.ParfaitId
import kotlinx.datetime.LocalDate

/** 목록·달력이 쓰는 만큼의 파르페 정보. 상세 화면이 필요로 하는 것은 따로 받는다 */
data class ParfaitHistory(
    val parfaitId: ParfaitId,
    val date: LocalDate,
    val thumbnailUrl: String?,
    val imageCount: Int,
) {
    /** 캔버스만 열어 보고 이미지는 안 올린 날. 달력이 점을 찍으면 안 되는 날이다 */
    val isEmpty: Boolean = imageCount == 0
}
