package com.teamyg.feature.sample

import com.teamyg.core.ui.UiSideEffect

sealed interface UserSideEffect : UiSideEffect {
    data class ShowToast(val message: String) : UserSideEffect
}
