package com.teamyg.feature.sample

import androidx.lifecycle.viewModelScope
import com.tjyg.core.ui.BaseViewModel
import com.tjyg.core.ui.UserIntent
import com.tjyg.core.ui.UserSideEffect
import com.tjyg.core.ui.UserState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor() : BaseViewModel<UserState, UserIntent, UserSideEffect>(
    UserState()
) {
    override fun processIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUser -> loadUser()
            is UserIntent.UpdateName -> updateState { copy(userName = intent.name) }
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            updateState { copy(isLoading = false, userName = "TEAM YG") }
            postSideEffect(UserSideEffect.showToast("불러오기 성공"))
        }
    }
}