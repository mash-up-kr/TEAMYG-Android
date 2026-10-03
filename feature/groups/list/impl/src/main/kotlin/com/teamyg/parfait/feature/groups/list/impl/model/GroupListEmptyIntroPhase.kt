package com.teamyg.parfait.feature.groups.list.impl.model

internal enum class GroupListEmptyIntroPhase {
    Entering,
    Shown,
    Dismissing,
    Dismissed,
    ;

    /** 등장이 끝나기 전의 터치는 무시한다. */
    fun onTouchDown(): GroupListEmptyIntroPhase = when (this) {
        Shown -> Dismissing
        else -> this
    }

    /** 진행 중인 애니메이션을 끝 상태로 건너뛴다. */
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
