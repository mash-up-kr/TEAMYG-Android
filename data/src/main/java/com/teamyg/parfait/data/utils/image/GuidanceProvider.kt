package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.domain.model.SegmentationBounds

/**
 * 정련이 쓸 안내자를 공급한다. 커널이 `Bitmap` 을 모른다는 원칙을 지키면서 두 경로가 서로 다른
 * 방식으로 픽셀을 대게 하는 통로다.
 */
internal fun interface GuidanceProvider {
    /** [bounds] 크기의 ARGB. 행 우선이고 stride 는 `bounds.width` 다 */
    fun pixelsIn(bounds: SegmentationBounds): IntArray
}
