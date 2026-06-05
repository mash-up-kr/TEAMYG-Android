package com.teamyg.feature.sample

import com.teamyg.core.ui.UiIntent

sealed interface UserIntent : UiIntent {
    object LoadUser : UserIntent

    data class UpdateName(val name: String) : UserIntent
}
