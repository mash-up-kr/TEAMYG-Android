package com.teamyg.parfait.feature.groups.list.impl.model

internal enum class GroupListEmptyIntroPhase {
    Entering,
    Shown,
    Dismissing,
    Dismissed,
    ;

    fun onTouchDown(): GroupListEmptyIntroPhase = when (this) {
        Shown -> Dismissing
        else -> this
    }

    fun onClickAddGroup(): GroupListEmptyIntroPhase = when (this) {
        Entering -> Shown
        else -> this
    }

    fun onStop(): GroupListEmptyIntroPhase = when (this) {
        Entering -> Shown
        Dismissing -> Dismissed
        else -> this
    }

    fun enterValue(animated: Float): Float = when (this) {
        Entering -> animated
        else -> 1f
    }

    fun exitValue(animated: Float): Float = when (this) {
        Entering, Shown -> 1f
        Dismissing -> animated
        Dismissed -> 0f
    }
}
