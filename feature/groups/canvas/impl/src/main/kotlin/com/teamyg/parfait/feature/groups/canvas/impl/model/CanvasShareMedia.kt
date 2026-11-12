package com.teamyg.parfait.feature.groups.canvas.impl.model

/** 인스타그램 스토리로 넘기는 미디어 종류. 스토리 인텐트에 실을 MIME 타입을 함께 든다 */
internal enum class CanvasShareMedia(
    val mimeType: String,
) {
    IMAGE("image/*"),
    VIDEO("video/*"),
}
