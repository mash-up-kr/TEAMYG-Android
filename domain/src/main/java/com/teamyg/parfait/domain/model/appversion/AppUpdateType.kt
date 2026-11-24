package com.teamyg.parfait.domain.model.appversion

/** 지금은 [FORCE] 만 앱 진입을 막는다. [OPTIONAL] 은 선택 업데이트 안내 UI 를 붙일 때를 위해 남긴다 */
enum class AppUpdateType {
    FORCE,
    OPTIONAL,
    NONE,
    UNKNOWN,
}
