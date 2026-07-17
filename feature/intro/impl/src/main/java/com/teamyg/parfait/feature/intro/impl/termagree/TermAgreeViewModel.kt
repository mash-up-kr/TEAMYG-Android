package com.teamyg.parfait.feature.intro.impl.termagree

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.feature.intro.impl.termagree.TermAgreeSideEffect.*
import com.teamyg.parfait.feature.intro.impl.termagree.model.TERM_CONTENT_LIST
import com.teamyg.parfait.feature.intro.impl.termagree.model.TermContent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class TermAgreeState(
    val termContentList: List<TermContent> = TERM_CONTENT_LIST,
    val selectedList: List<Boolean> = List(termContentList.size) { false },
) : UiState {
    val isAllSelected: Boolean = selectedList.all { it }
    val isAvailable: Boolean = termContentList
        .withIndex()
        .none { it.value.isRequired && selectedList.getOrNull(it.index) == false }
}

sealed interface TermAgreeIntent : UiIntent {
    data class ClickTermAgree(val index: Int, val newSelected: Boolean) : TermAgreeIntent

    data class ClickTermLandingUrl(val landingUrl: String?) : TermAgreeIntent

    data class ClickAgreeAllTerm(val newSelected: Boolean) : TermAgreeIntent

    data object ClickNextButton : TermAgreeIntent

    data object ClickBackButton : TermAgreeIntent
}

sealed interface TermAgreeSideEffect : UiSideEffect {
    data class NavigateToUrl(val landingUrl: String) : TermAgreeSideEffect

    data object NavigateToBack : TermAgreeSideEffect

    data object NavigateToNext : TermAgreeSideEffect
}

@HiltViewModel
class TermAgreeViewModel
@Inject
constructor() : BaseViewModel<TermAgreeState, TermAgreeIntent, TermAgreeSideEffect>(initialState = TermAgreeState()) {
    init {
        viewModelLogger.i { "TermAgreeViewModel::init" }
    }

    override fun processIntent(intent: TermAgreeIntent) {
        when (intent) {
            is TermAgreeIntent.ClickAgreeAllTerm -> {
                updateState { copy(selectedList = List(termContentList.size) { intent.newSelected }) }
            }

            is TermAgreeIntent.ClickTermAgree -> {
                updateState {
                    copy(
                        selectedList = selectedList.mapIndexed { index, selected ->
                            if (index ==
                                intent.index
                            ) {
                                intent.newSelected
                            } else {
                                selected
                            }
                        },
                    )
                }
            }

            is TermAgreeIntent.ClickTermLandingUrl -> {
                intent.landingUrl?.let { landingUrl ->
                    postSideEffect(NavigateToUrl(landingUrl))
                }
            }

            TermAgreeIntent.ClickBackButton -> {
                postSideEffect(NavigateToBack)
            }

            TermAgreeIntent.ClickNextButton -> {
                postSideEffect(NavigateToNext)
            }
        }
    }
}
