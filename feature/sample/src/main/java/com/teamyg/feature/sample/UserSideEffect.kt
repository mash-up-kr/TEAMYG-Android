package com.teamyg.feature.sample

import com.tjyg.core.ui.UiSideEffect

sealed interface UserSideEffect : UiSideEffect {
    data class ShowToast(val message: String) : UserSideEffect
}
