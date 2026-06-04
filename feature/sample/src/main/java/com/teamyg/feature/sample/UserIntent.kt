package com.teamyg.feature.sample

import com.tjyg.core.ui.mvi.UiIntent

sealed interface UserIntent : UiIntent {
    object LoadUser : UserIntent

    data class UpdateName(val name: String) : UserIntent
}
