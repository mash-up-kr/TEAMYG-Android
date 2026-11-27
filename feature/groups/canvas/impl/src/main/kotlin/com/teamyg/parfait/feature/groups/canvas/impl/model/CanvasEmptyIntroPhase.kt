package com.teamyg.parfait.feature.groups.canvas.impl.model

enum class CanvasEmptyIntroPhase {
    Idle,
    Entering,
    Shown,
    Dismissing,
    Dismissed,
    ;

    val isPlaying: Boolean
        get() = this == Entering || this == Shown || this == Dismissing

    fun onAnimationFinished(): CanvasEmptyIntroPhase = when (this) {
        Entering -> Shown
        else -> this
    }

    fun onTap(): CanvasEmptyIntroPhase = when (this) {
        Entering, Shown -> Dismissing
        else -> this
    }

    fun onInteraction(): CanvasEmptyIntroPhase = when (this) {
        Entering -> Shown
        Shown -> Dismissing
        else -> this
    }

    fun onStop(): CanvasEmptyIntroPhase = when (this) {
        Entering -> Shown
        Dismissing -> Dismissed
        else -> this
    }

    fun onDismissFinished(): CanvasEmptyIntroPhase = when (this) {
        Dismissing -> Dismissed
        else -> this
    }

    fun enterValue(animated: Float): Float = when (this) {
        Idle, Dismissed -> 0f
        Entering, Dismissing -> animated
        Shown -> 1f
    }

    fun exitValue(animated: Float): Float = when (this) {
        Idle, Entering, Shown -> 1f
        Dismissing -> animated
        Dismissed -> 0f
    }
}
