package com.teamyg.parfait.feature.groups.canvas.impl.model

/** [groupId]·[parfaitId] 는 입력 화면이 배경으로 그릴 오늘 캔버스를 구독하는 데 쓴다 */
data class TextEditSession(
    val groupId: Long,
    val parfaitId: Long,
    val original: EditableText,
)
