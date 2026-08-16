package com.teamyg.parfait.domain.model.session

/**
 * 앱 진입 시 [com.teamyg.parfait.domain.usecase.session.BootstrapSessionUseCase] 가 내리는
 * 목적지.
 *
 * 스플래시 화면이 "토큰이 있나", "조회가 됐나"를 알 필요가 없게 하려고 도메인 타입으로
 * 돌려준다 — 판단은 도메인이 하고 화면은 결과를 내비게이션으로 옮기기만 한다.
 */
sealed interface SessionBootstrap {
    /** SSoT 가 채워진 상태로 그룹 목록에 도착한다 */
    data object ToGroupList : SessionBootstrap

    data object ToLogin : SessionBootstrap
}
