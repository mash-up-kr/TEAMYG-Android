package com.teamyg.parfait.data.model.image

import android.graphics.Bitmap
import com.teamyg.parfait.domain.model.SegmentationBounds

/** 판 한 장의 출처 */
internal sealed interface PlateSource {
    /** 검출 공간이 곧 원본 공간이다 */
    class MlKitPlate(val plate: Bitmap, val region: SegmentationBounds) : PlateSource
}
