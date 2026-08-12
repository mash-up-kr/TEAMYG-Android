package com.teamyg.parfait.feature.groups.enter.impl.nickname

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.group.EnterGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupNickNameUiState(
    val nickName: String = "",
    val nicknameError: NameValidResult.Error? = null,
    val isEntering: Boolean = false,
) : UiState

sealed interface GroupNickNameIntent : UiIntent {
    data object ClickNextButton : GroupNickNameIntent

    data object ClickBackButton : GroupNickNameIntent

    data class InputWord(val nickName: String) : GroupNickNameIntent
}

sealed interface GroupNickNameSideEffect : UiSideEffect {
    data object NavigateToBack : GroupNickNameSideEffect

    data object NavigateToNext : GroupNickNameSideEffect
}

@HiltViewModel
class GroupNickNameViewModel
@Inject
constructor(
    private val checkNickNameValid: CheckNameValidUseCase,
    private val enterGroup: EnterGroupUseCase,
) : BaseViewModel<GroupNickNameUiState, GroupNickNameIntent, GroupNickNameSideEffect>(
    initialState = GroupNickNameUiState(),
) {
    override fun processIntent(intent: GroupNickNameIntent) {
        when (intent) {
            GroupNickNameIntent.ClickBackButton -> postSideEffect(GroupNickNameSideEffect.NavigateToBack)

            is GroupNickNameIntent.ClickNextButton -> {
                if (state.value.isEntering) return

                when (val result = checkNickNameValid(state.value.nickName)) {
                    NameValidResult.Success -> {
                        updateState {
                            copy(
                                nicknameError = null,
                                isEntering = true,
                            )
                        }
                        viewModelScope.launch {
                            val enterResult = enterGroup(nickName = state.value.nickName)
                            updateState { copy(isEntering = false) }

                            // Todo : 실패 처리는 서버 작업이 연결되면 추가 예정입니다
                            if (enterResult.isSuccess) {
                                postSideEffect(GroupNickNameSideEffect.NavigateToNext)
                            }
                        }
                    }

                    is NameValidResult.Error -> {
                        updateState {
                            copy(nicknameError = result)
                        }
                    }
                }
            }

            is GroupNickNameIntent.InputWord -> {
                updateState {
                    copy(
                        nickName = intent.nickName,
                        nicknameError = null,
                    )
                }
            }
        }
    }
}
