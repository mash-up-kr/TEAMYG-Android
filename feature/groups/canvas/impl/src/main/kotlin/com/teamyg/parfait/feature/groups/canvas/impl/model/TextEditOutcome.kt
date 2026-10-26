package com.teamyg.parfait.feature.groups.canvas.impl.model

/** 텍스트 재편집 왕복이 남긴 결과 */
data class TextEditOutcome(
    val session: TextEditSession,
    val result: TextEditResult,
) {
    val parfaitTextId: Long get() = session.original.parfaitTextId
}
