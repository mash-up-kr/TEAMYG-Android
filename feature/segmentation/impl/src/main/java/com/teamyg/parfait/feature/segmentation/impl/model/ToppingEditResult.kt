package com.teamyg.parfait.feature.segmentation.impl.model

import com.teamyg.parfait.domain.model.image.SourceLongSide

/**
 * @param subjectImagePath 테두리를 두르지 않은 알맹이. 투명 여백을 걷어 실제 토핑 크기다
 * @param cutoutImagePath 원본 좌표계를 지키려고 여백을 걷지 않은 판
 * @param sourceLongSide 원본 사진 전체의 긴 변(px)
 */
internal data class ToppingEditResult(
    val subjectImagePath: String,
    val cutoutImagePath: String,
    val sourceLongSide: SourceLongSide,
)
