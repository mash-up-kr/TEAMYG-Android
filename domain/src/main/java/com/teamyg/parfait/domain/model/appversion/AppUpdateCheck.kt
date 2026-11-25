package com.teamyg.parfait.domain.model.appversion

sealed interface AppUpdateCheck {
    /** 강제 업데이트. 업데이트하거나 앱을 끝내는 것 말고는 진입할 길이 없다 */
    data class Required(val storeUrl: String?) : AppUpdateCheck

    /** 선택 업데이트. 사용자가 미루면 그대로 진입한다 */
    data class Optional(val storeUrl: String?) : AppUpdateCheck

    data object NotRequired : AppUpdateCheck
}
