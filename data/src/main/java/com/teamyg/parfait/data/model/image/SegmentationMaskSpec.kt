package com.teamyg.parfait.data.model.image

internal object SegmentationMaskSpec {
    /** 이 신뢰도 이하는 완전히 투명하다 */
    const val RAMP_FLOOR = 0.35f

    /** 이 신뢰도 이상은 완전히 불투명하다 */
    const val RAMP_CEILING = 0.65f

    const val FULLY_OPAQUE = 255
}
