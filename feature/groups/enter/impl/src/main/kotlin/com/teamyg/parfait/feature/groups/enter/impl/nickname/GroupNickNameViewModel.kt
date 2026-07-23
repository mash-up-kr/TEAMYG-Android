package com.teamyg.parfait.feature.groups.enter.impl.nickname

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.usecase.group.CheckNickNameValidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class GroupNickNameUiState(
    val nickName: String = "",
    val errorMessage: String? = null,
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
    private val checkNickNameValid: CheckNickNameValidUseCase,
) : BaseViewModel<GroupNickNameUiState, GroupNickNameIntent, GroupNickNameSideEffect>(
    initialState = GroupNickNameUiState(),
) {
    override fun processIntent(intent: GroupNickNameIntent) {
        when (intent) {
            GroupNickNameIntent.ClickBackButton -> postSideEffect(GroupNickNameSideEffect.NavigateToBack)

            is GroupNickNameIntent.ClickNextButton -> {
                val result = checkNickNameValid(state.value.nickName)
                when (result) {
                    NameValidResult.Success -> {
                        updateState {
                            copy(errorMessage = null)
                        }
                        postSideEffect(GroupNickNameSideEffect.NavigateToNext)
                    }

                    NameValidResult.Error.DuplicatedSpace -> {
                        updateState {
                            copy(errorMessage = "공백은 글자 사이에 1칸만 사용할 수 있어요")
                        }
                    }

                    NameValidResult.Error.InvalidCharacter -> {
                        updateState {
                            copy(errorMessage = "한글, 영문, 숫자, 띄어쓰기만 사용할 수 있어요")
                        }
                    }

                    NameValidResult.Error.SpaceAtEdge -> {
                        updateState {
                            copy(errorMessage = "닉네임의 처음과 끝에는 공백을 사용할 수 없어요")
                        }
                    }
                }
            }

            is GroupNickNameIntent.InputWord -> {
                updateState {
                    copy(
                        nickName = intent.nickName,
                        errorMessage = null,
                    )
                }
            }
        }
    }
}
