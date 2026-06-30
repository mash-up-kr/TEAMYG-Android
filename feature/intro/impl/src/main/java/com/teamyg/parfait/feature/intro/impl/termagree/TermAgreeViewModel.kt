package com.teamyg.parfait.feature.intro.impl.termagree

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.feature.intro.impl.termagree.model.TERM_CONTENT_LIST
import com.teamyg.parfait.feature.intro.impl.termagree.model.TermContent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class TermAgreeState(
    val termContent: List<TermContent> = TERM_CONTENT_LIST,
    val selectedList: List<Boolean> = emptyList(),
) : UiState {
    val isAllSelected: Boolean = selectedList.all { it }
}

sealed interface TermAgreeIntent : UiIntent

sealed interface TermAgreeSideEffect : UiSideEffect

@HiltViewModel
class TermAgreeViewModel
@Inject
constructor(
) : BaseViewModel<TermAgreeState, TermAgreeIntent, TermAgreeSideEffect>(initialState = TermAgreeState()) {
    init {
        viewModelLogger.i { "TermAgreeViewModel::init" }
    }

    override fun processIntent(intent: TermAgreeIntent) = Unit
}
