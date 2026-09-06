package com.teamyg.parfait.domain.model.member

/**
 * 화면 첫 진입에서 한 번만 도는 튜토리얼의 종류.
 *
 * 화면마다 플래그를 따로 두는 이유: 한 개로 묶으면 캔버스 튜토리얼을 본 사람에게 업로드·누끼
 * 튜토리얼까지 이미 본 것으로 처리된다 — 세 화면은 서로 다른 시점에 처음 열린다.
 *
 * 이름이 곧 저장 키다([com.teamyg.parfait.domain.model.member.UserConfigVO]). 이미 나간 항목의
 * 이름을 바꾸면 그 튜토리얼을 끝낸 기록이 사라져 다시 뜬다.
 */
enum class TutorialKind {
    CANVAS,
    UPLOAD,
    SEGMENTATION,
}
