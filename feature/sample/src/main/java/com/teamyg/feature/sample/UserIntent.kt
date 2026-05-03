package com.teamyg.feature.sample

import com.tjyg.core.ui.UiIntent

sealed interface UserIntent : UiIntent {
    object LoadUser : UserIntent
    data class UpdateName(val name: String) : UserIntent
}