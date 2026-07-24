package com.teamyg.parfait.feature.groups.enter.impl.nickname

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.teamyg.parfait.core.ui.R as CoreR

data class GroupNickNameUiState(
    val nickName: String = "",
    val errorMessageResId: Int? = null,
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
                            copy(errorMessageResId = null)
                        }
                        postSideEffect(GroupNickNameSideEffect.NavigateToNext)
                    }

                    NameValidResult.Error.DuplicatedSpace -> {
                        updateState {
                            copy(errorMessageResId = CoreR.string.error_duplicated_space)
                        }
                    }

                    NameValidResult.Error.InvalidCharacter -> {
                        updateState {
                            copy(errorMessageResId = CoreR.string.error_invalid_character)
                        }
                    }

                    NameValidResult.Error.SpaceAtEdge -> {
                        updateState {
                            copy(errorMessageResId = CoreR.string.error_space_at_edge_nickname)
                        }
                    }

                    NameValidResult.Error.EmptyString -> {
                        updateState {
                            copy(errorMessageResId = CoreR.string.error_empty_space_nickname)
                        }
                    }
                }
            }

            is GroupNickNameIntent.InputWord -> {
                updateState {
                    copy(
                        nickName = intent.nickName,
                        errorMessageResId = null,
                    )
                }
            }
        }
    }
}
