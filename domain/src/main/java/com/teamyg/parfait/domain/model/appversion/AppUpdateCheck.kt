package com.teamyg.parfait.domain.model.appversion

sealed interface AppUpdateCheck {
    data class Required(val storeUrl: String?) : AppUpdateCheck

    data object NotRequired : AppUpdateCheck
}
