package com.teamyg.parfait.feature.groups.canvas.impl.model

data class TextEditOutcome(
    val session: TextEditSession,
    val result: TextEditResult,
) {
    val parfaitTextId: Long get() = session.original.parfaitTextId
}
