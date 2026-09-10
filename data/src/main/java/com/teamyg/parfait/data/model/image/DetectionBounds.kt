package com.teamyg.parfait.data.model.image

/**
 * 검출 공간의 사각형.
 *
 * `SegmentationBounds` 는 KDoc 이 원본 좌표를 단정하고 있어, 같은 타입으로 두 좌표계를 겸하면 짝이 안 맞는
 * 조합이 컴파일된다.
 */
internal data class DetectionBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)
