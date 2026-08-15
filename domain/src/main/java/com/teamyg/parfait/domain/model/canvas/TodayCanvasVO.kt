package com.teamyg.parfait.domain.model.canvas

import com.teamyg.parfait.domain.model.id.ParfaitId
import kotlinx.datetime.LocalDate

/**
 * 오늘의 캔버스 전체.
 *
 * ⚠️ 이 값을 얻는 조회는 서버에서 캔버스 행을 만든다 — 해당 날짜 파르페가 없으면 생성해
 * 저장한다(`api/parfait.md`). 화면이 이 호출을 남발하면 빈 캔버스가 양산된다.
 *
 * lastClosedDate 는 CLOSED 만 센다(EMPTY 제외) — "마지막 마감일"이 아니라
 * "마지막으로 토핑이 있던 날"이다.
 *
 * toppings 는 서버가 0건일 때 null 을 주지만 여기서는 빈 목록으로 접는다 — 0건과 빈 목록은
 * 같은 뜻이라 소비처가 널 분기를 반복할 이유가 없다. background·lastClosedDate 의 null 은
 * "미설정"·"이력 없음"이라는 의미가 있어 그대로 둔다.
 */
data class TodayCanvasVO(
    val parfaitId: ParfaitId,
    val date: LocalDate,
    val status: CanvasStatus,
    val lastClosedDate: LocalDate?,
    val members: List<CanvasMemberVO>,
    val background: CanvasBackground?,
    val toppings: List<CanvasToppingVO>,
)
