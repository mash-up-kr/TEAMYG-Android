package com.teamyg.parfait.core.ui.mvi.extension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.mvi.MviContainer
import com.teamyg.parfait.core.ui.mvi.impl.YGMviContainer
import com.teamyg.parfait.core.ui.mvi.scope.IntentScope
import com.teamyg.parfait.core.ui.mvi.setting.MviSettings

fun <STATE, EFFECT> ViewModel.container(
    initialState: STATE,
    settings: MviSettings = MviSettings(),
    onCreate: (suspend IntentScope<STATE, EFFECT>.() -> Unit)? = null,
): MviContainer<STATE, EFFECT> = YGMviContainer(
    initialState = initialState,
    scope = viewModelScope,
    settings = settings,
    onCreate = onCreate,
)
