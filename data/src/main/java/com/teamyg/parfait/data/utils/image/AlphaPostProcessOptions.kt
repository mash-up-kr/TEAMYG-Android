package com.teamyg.parfait.data.utils.image

internal data class AlphaPostProcessOptions(
    val downscaleFactor: Int = 4,
    val binaryThreshold: Int = 127,
    val areaOpeningMinPixels: Int = AREA_OPENING_MIN_PIXELS,
    val erodeEdge: Boolean = true,
    val minPixelsForDownscale: Int = MIN_PIXELS_FOR_DOWNSCALE,
    val refineEdges: Boolean = true,
    val refineDownscale: Int = REFINE_DOWNSCALE,
    val refineRadius: Int = REFINE_RADIUS,
    val refineEpsilon: Float = REFINE_EPSILON,
) {
    init {
        // 배율은 판정 좌표를 나누는 데 네 자리에서 쓰인다. 만들어지는 자리에서 막아야
        // 어느 자리가 터지든 원인이 이 값이라는 것이 드러난다
        require(downscaleFactor >= 1) { "downscaleFactor must be >= 1 but was $downscaleFactor" }
    }
}
