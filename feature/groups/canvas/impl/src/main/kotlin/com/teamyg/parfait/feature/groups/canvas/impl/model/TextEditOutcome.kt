package com.teamyg.parfait.feature.groups.canvas.impl.model

/**
 * 텍스트 재편집 왕복이 남긴 결과. 어느 편집이 낳은 결과인지 [session] 이 함께 들고 있다.
 */
data class TextEditOutcome(
    val session: TextEditSession,
    val result: TextEditResult,
) {
    /** 편집한 텍스트의 id */
    val parfaitTextId: Long get() = session.original.parfaitTextId
}
