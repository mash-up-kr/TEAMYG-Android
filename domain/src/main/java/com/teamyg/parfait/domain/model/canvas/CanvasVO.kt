package com.teamyg.parfait.domain.model.canvas

import com.teamyg.parfait.domain.model.id.ParfaitId
import kotlinx.datetime.LocalDate

/**
 * 캔버스 전체. 오늘의 캔버스와 특정 캔버스 상세가 같은 형태다 — 서버가 두 조회에 같은 응답을
 * 쓰기 때문이고, 그래서 이 타입은 날짜를 가리지 않는다.
 *
 * ⚠️ 오늘의 캔버스 조회만 서버에서 캔버스 행을 만든다 — 해당 날짜 파르페가 없으면 생성해
 * 저장한다(`api/parfait.md`). 화면이 그 호출을 남발하면 빈 캔버스가 양산된다.
 * 상세 조회는 부작용이 없다.
 *
 * lastClosedDate 는 CLOSED 만 센다(EMPTY 제외) — "마지막 마감일"이 아니라
 * "마지막으로 토핑이 있던 날"이다.
 *
 * toppings 는 서버가 0건일 때 null 을 주지만 여기서는 빈 목록으로 접는다 — 0건과 빈 목록은
 * 같은 뜻이라 소비처가 널 분기를 반복할 이유가 없다. background·lastClosedDate 의 null 은
 * "미설정"·"이력 없음"이라는 의미가 있어 그대로 둔다.
 */
data class CanvasVO(
    val parfaitId: ParfaitId,
    val date: LocalDate,
    val status: CanvasStatus,
    val lastClosedDate: LocalDate?,
    val members: List<CanvasMemberVO>,
    val background: CanvasBackground?,
    val toppings: List<CanvasToppingVO>,
)
