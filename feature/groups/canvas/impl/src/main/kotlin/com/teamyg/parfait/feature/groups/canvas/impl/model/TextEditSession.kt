package com.teamyg.parfait.feature.groups.canvas.impl.model

/**
 * 토핑 정리 화면이 텍스트 입력 화면에 건네는 편집 대상. [groupId]·[parfaitId] 는 입력 화면이
 * 배경으로 그릴 오늘 캔버스를 구독하는 데 쓴다 — 편집 모드는 토핑 초안을 읽지 않는다.
 */
data class TextEditSession(
    val groupId: Long,
    val parfaitId: Long,
    val original: EditableText,
)
