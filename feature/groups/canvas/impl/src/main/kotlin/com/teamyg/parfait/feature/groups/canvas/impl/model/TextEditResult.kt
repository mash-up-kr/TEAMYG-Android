package com.teamyg.parfait.feature.groups.canvas.impl.model

/**
 * 텍스트 입력 화면이 편집을 마치고 남기는 결과. 서버에는 아직 아무것도 보내지 않은 상태다 —
 * 저장은 토핑 정리 화면의 확인이 맡는다.
 *
 * [Deleted] 는 내용을 비운 채 완료한 경우다.
 */
sealed interface TextEditResult {
    /** 위치는 [EditableText] 와 같이 박스 중심의 Canvas-Area 대비 비율이다 */
    data class Edited(
        val text: String,
        val color: TextToppingColor,
        val type: TextToppingType,
        val designFontSize: Float,
        val positionX: Float,
        val positionY: Float,
        val scale: Float,
        val rotationDegrees: Float,
    ) : TextEditResult

    data object Deleted : TextEditResult
}
