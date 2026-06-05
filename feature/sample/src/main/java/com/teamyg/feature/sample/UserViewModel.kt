package com.teamyg.feature.sample

import androidx.lifecycle.viewModelScope
import com.teamyg.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel
    @Inject
    constructor() :
    BaseViewModel<UserState, UserIntent, UserSideEffect>(initialState = UserState()) {
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
                postSideEffect(UserSideEffect.ShowToast("불러오기 성공"))
            }
        }
    }
