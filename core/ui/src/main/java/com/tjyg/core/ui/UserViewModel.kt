package com.tjyg.core.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class UserViewModel : BaseViewModel<UserState, UserIntent, UserSideEffect>(
    UserState()
){
    override fun processIntent(intent: UserIntent) {
        when(intent){
            is UserIntent.LoadUser -> loadUser()
            is UserIntent.UpdateName -> updateState { copy(userName = intent.name) }
        }
    }
    private fun loadUser(){
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            updateState { copy(isLoading = false, userName = "TEAM YG") }
            postSideEffect(UserSideEffect.showToast("불러오기 성공"))
        }
    }
}