package com.teamyg.parfait.feature.segmentation.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Segmentation 결과를 토핑으로 쓰기 전에 손으로 다듬는 편집 화면의 진입점.
 *
 * @param sourceImageUri 원본 이미지. 제거했던 영역을 다시 채울 때 이 픽셀을 가져온다
 * @param segmentationImageUri Segmentation 으로 잘라낸 이미지. 이 이미지의 알파가 편집의 시작 마스크가 된다
 */
@Serializable
data class NavKeyToppingEdit(
    val sourceImageUri: String,
    val segmentationImageUri: String,
) : NavKey

/**
 * 편집 결과.
 *
 * @param subjectImagePath 테두리를 두르지 않은 알맹이. 투명 여백을 걷어 실제 토핑 크기다
 * @param cutoutImagePath 다시 편집할 때의 시작 마스크. 원본 좌표계를 지켜야 해 여백을 걷지 않는다
 * @param sourceLongSide 원본 사진 전체의 긴 변(px). 이 모듈은 `domain`을 의존하지 않으므로
 * `SourceLongSide` 값 타입이 아니라 벌거벗은 `Int`로 나른다 (근거는 `adr/0002-feature-api-impl-split.md`)
 */
data class ToppingEditResult(
    val subjectImagePath: String,
    val cutoutImagePath: String,
    val sourceLongSide: Int,
)

/**
 * 편집 화면이 결과를 돌려줄 때 쓰는 결과 키.
 *
 * 결과 타입은 [ToppingEditResult] 다. [NavKeyToppingEdit] 로 들어온 쪽이 이 키로 결과를 받는다.
 */
const val TOPPING_EDIT_RESULT_KEY = "topping_edit_result"
