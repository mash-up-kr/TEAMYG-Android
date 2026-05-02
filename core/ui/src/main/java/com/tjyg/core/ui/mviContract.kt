package com.tjyg.core.ui

sealed interface UiState
sealed interface UiIntent
sealed interface UiSideEffect

data class UserState(
    val isLoading: Boolean = false, val userName: String = "", val error: String? = null
) : UiState

sealed interface UserIntent : UiIntent {
    object LoadUser : UserIntent
    data class UpdateName(val name: String) : UserIntent
}

sealed class UserSideEffect : UiSideEffect {
    data class showToast(val message: String) : UserSideEffect()
}