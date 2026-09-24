package com.teamyg.parfait.data.utils.image

import android.graphics.Bitmap
import com.teamyg.parfait.data.model.image.ProjectedRegion
import com.teamyg.parfait.domain.model.SegmentationBounds

/** 판 한 장이 어디서 오는가. 어느 쪽을 쓸지는 [harvestSubjects] 가 투영 유무로 정한다 */
internal sealed interface PlateSource {
    /** 1차 경로 전용. 검출 공간이 곧 원본 공간이라 이 판의 픽셀이 원본 색이다 */
    class MlKitPlate(val plate: Bitmap, val region: SegmentationBounds) : PlateSource

    /**
     * 회복 경로 전용. 픽셀 인자가 없다 — 알파만 [detectionPlate] 에서 가져오고 픽셀은 원본에서 읽는다.
     * 검출 판은 대비를 건 판이라 그 픽셀을 쓰면 결과 색이 변한다.
     */
    class OriginRegion(val detectionPlate: Bitmap, val projected: ProjectedRegion) : PlateSource
}
