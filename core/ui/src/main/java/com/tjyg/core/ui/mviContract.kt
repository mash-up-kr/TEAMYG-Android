package com.tjyg.core.ui

interface UiState
interface UiIntent
interface UiSideEffect

data class UserState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val error: String? = null
) : UiState

sealed class UserIntent : UiIntent {
    object LoadUser : UserIntent()
    data class UpdateName(val name: String) : UserIntent()
}

sealed class UserSideEffect : UiSideEffect {
    data class showToast(val message: String) : UserSideEffect()
}