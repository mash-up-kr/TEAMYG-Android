package com.teamyg.parfait.domain.model.member

/**
 * 서버가 아니라 기기에만 남는 사용자 설정.
 *
 * @param isShowCanvasTutorial 캔버스 튜토리얼을 아직 보여줘야 하는가. 앱을 막 설치해 저장분이
 *   없는 상태와 같은 뜻이 되도록 기본값이 `true` 다
 */
data class UserConfigVO(
    val isShowCanvasTutorial: Boolean = true,
)
