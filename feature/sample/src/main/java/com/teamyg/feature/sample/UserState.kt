package com.teamyg.feature.sample

import com.teamyg.core.ui.UiState

data class UserState(val isLoading: Boolean = false, val userName: String = "", val error: String? = null) : UiState
