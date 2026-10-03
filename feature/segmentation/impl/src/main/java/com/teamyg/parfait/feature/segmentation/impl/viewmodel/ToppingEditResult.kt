package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import com.teamyg.parfait.domain.model.image.SourceLongSide

/**
 * 편집 결과.
 *
 * @param subjectImagePath 테두리를 두르지 않은 알맹이. 투명 여백을 걷어 실제 토핑 크기다
 * @param cutoutImagePath 다시 편집할 때의 시작 마스크. 원본 좌표계를 지켜야 해 여백을 걷지 않는다
 * @param sourceLongSide 원본 사진 전체의 긴 변(px)
 */
internal data class ToppingEditResult(
    val subjectImagePath: String,
    val cutoutImagePath: String,
    val sourceLongSide: SourceLongSide,
)
