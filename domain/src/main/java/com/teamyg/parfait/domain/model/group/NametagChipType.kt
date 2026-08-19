package com.teamyg.parfait.domain.model.group

/**
 * 그룹 안에서 사람을 가리키는 칩 타입. 배정 주체는 **서버**다 — 참여·생성 시 그 그룹의 활동
 * 멤버가 안 쓰는 값 중 하나를 받고, 그룹을 나가면 [DEFAULT] 로 반납된다. 다시 뽑는 경로는 없다.
 *
 * 유일성은 **그룹 안에서만** 성립한다 — 같은 사람이 그룹마다 다른 타입을 받는다(닉네임 초기값이
 * 계정 공통인 것과 반대다).
 *
 * 화면 색으로 옮기는 일은 feature 가 한다. `:domain` 은 `:core:designsystem` 을 모른다.
 */
enum class NametagChipType {
    TYPE1,
    TYPE2,
    TYPE3,
    TYPE4,
    TYPE5,
    TYPE6,
    TYPE7,
    TYPE8,
    TYPE9,
    TYPE10,
    TYPE11,
    TYPE12,

    /**
     * 그룹을 나간 사람이 반납한 자리. 12종과 달리 **유일성 제약이 없어** 한 그룹 안에서 여럿이
     * 동시에 가질 수 있다(서버 NameTagChipType 이 이 성질을 명시한다).
     *
     * **앱이 모르는 문자열도 매퍼가 여기로 접는다** — 그래서 이 값이 왔다고 반드시 "나간 사람"인
     * 것은 아니다. 그렇게 정한 사유와 그 대가는 `parfait/adr/0024-nametag-chip-unknown-fold.md`.
     */
    DEFAULT,
}
