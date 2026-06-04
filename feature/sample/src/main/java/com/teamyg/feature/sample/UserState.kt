package com.teamyg.feature.sample

import com.tjyg.core.ui.mvi.UiState

data class UserState(val isLoading: Boolean = false, val userName: String = "", val error: String? = null) : UiState
