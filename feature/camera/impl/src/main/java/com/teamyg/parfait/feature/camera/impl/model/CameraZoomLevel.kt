package com.teamyg.parfait.feature.camera.impl.model

/**
 * 줌 버튼이 순환하는 배율 단계. 선언 순서가 순환 순서다(Figma `BL_06`: 1x > 2x > 0.5x).
 */
enum class CameraZoomLevel(
    val ratio: Float,
    val label: String,
) {
    ONE(ratio = 1f, label = "1x"),
    TWO(ratio = 2f, label = "2x"),
    HALF(ratio = 0.5f, label = "0.5x"),
    ;

    fun next(available: List<CameraZoomLevel>): CameraZoomLevel {
        val index = available.indexOf(this)
        if (index == -1) return ONE

        return available[(index + 1) % available.size]
    }

    companion object {
        fun availableIn(range: ClosedFloatingPointRange<Float>): List<CameraZoomLevel> =
            entries.filter { it.ratio in range }
    }
}
