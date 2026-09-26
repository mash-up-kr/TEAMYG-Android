package com.teamyg.parfait.data.model.image

import android.graphics.Bitmap
import com.teamyg.parfait.domain.model.SegmentationBounds

/** 판 한 장의 출처 */
internal sealed interface PlateSource {
    /** 1차 경로 전용. 검출 공간이 곧 원본 공간이다 */
    class MlKitPlate(val plate: Bitmap, val region: SegmentationBounds) : PlateSource

    /** 회복 경로 전용. 알파만 [detectionPlate] 에서 가져온다. 대비를 건 판이라 픽셀은 원본에서 읽는다 */
    class OriginRegion(val detectionPlate: Bitmap, val projected: ProjectedRegion) : PlateSource
}
