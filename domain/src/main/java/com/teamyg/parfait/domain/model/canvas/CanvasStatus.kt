package com.teamyg.parfait.domain.model.canvas

/**
 * 캔버스 상태.
 *
 * EMPTY 는 "비어 있음"이 아니라 "빈 채로 마감됨"이다 — 03시 회전 배치가 토핑 0건인 캔버스를
 * 이 상태로 닫는다(`api/parfait.md`). ACTIVE 가 아니면 더 올릴 수 없다는 뜻이지만
 * 서버가 그것을 강제하지 않는다 — 마감된 캔버스에도 배치·수정·삭제가 통과한다.
 *
 * UNKNOWN 은 서버가 상태를 늘렸을 때 앱이 크래시하지 않게 하는 폴백이다.
 */
enum class CanvasStatus {
    ACTIVE,
    CLOSED,
    EMPTY,
    UNKNOWN,
}
