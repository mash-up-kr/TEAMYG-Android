package com.teamyg.parfait.domain.model.canvas

/**
 * 캔버스 상태.
 *
 * EMPTY 는 "비어 있음"이 아니라 "빈 채로 마감됨"이다 — 03시 회전 배치가 토핑 0건인 캔버스를
 * 이 상태로 닫는다(`api/parfait.md`). ACTIVE 가 아니면 더 올릴 수 없고 **서버도 그것을
 * 강제한다** — 토핑 배치·수정·테두리·삭제와 배경 변경이 409 PARFAIT_ALREADY_CLOSED 로
 * 거부된다(ServerErrorCode.Parfait.PARFAIT_ALREADY_CLOSED).
 *
 * UNKNOWN 은 서버가 상태를 늘렸을 때 앱이 크래시하지 않게 하는 폴백이다.
 */
enum class CanvasStatus {
    ACTIVE,
    CLOSED,
    EMPTY,
    UNKNOWN,
}
