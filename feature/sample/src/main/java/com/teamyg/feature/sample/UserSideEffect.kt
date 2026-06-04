package com.teamyg.feature.sample

import com.tjyg.core.ui.mvi.UiSideEffect

sealed interface UserSideEffect : UiSideEffect {
    data class ShowToast(val message: String) : UserSideEffect
}
