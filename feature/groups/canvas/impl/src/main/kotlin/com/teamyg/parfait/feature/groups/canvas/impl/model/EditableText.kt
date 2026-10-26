package com.teamyg.parfait.feature.groups.canvas.impl.model

/**
 * 토핑 정리 화면에 놓인 텍스트 하나. 위치 단위는 [EditableTopping] 과 같이 Canvas-Area 대비
 * 박스 **중심**의 비율이다.
 *
 * @param designFontSize 디자인 너비 기준 글자 크기. 화면 크기는 [scale] 을 곱한 값이다.
 */
data class EditableText(
    val parfaitTextId: Long,
    val isMine: Boolean,
    val text: String,
    val color: TextToppingColor,
    val type: TextToppingType,
    val designFontSize: Float,
    val positionX: Float,
    val positionY: Float,
    val positionZ: Int,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
)
