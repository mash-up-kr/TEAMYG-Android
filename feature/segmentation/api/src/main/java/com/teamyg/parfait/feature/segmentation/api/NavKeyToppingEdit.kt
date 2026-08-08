package com.teamyg.parfait.feature.segmentation.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Segmentation 결과를 토핑으로 쓰기 전에 손으로 다듬는 편집 화면의 진입점.
 *
 * @param sourceImageUri 원본 이미지. 제거했던 영역을 다시 채울 때 이 픽셀을 가져온다
 * @param segmentationImageUri Segmentation 으로 잘라낸 이미지. 이 이미지의 알파가 편집의 시작 마스크가 된다
 * @param borderLayers 이미 두른 테두리 겹. 다시 편집할 때 벗겨진 채로 열리지 않도록 되살릴 재료다
 */
@Serializable
data class NavKeyToppingEdit(
    val sourceImageUri: String,
    val segmentationImageUri: String,
    val borderLayers: List<ToppingBorderLayer> = emptyList(),
) : NavKey

/**
 * 편집 화면이 편집을 마치고 돌려주는 결과.
 *
 * 테두리는 [editedImagePath] 에 이미 구워져 있어 되짚을 수 없다.
 * 그래서 테두리를 두르기 전 알맹이와 두른 겹 목록을 함께 돌려주고, 다시 편집할 때 이 둘로 되살린다.
 *
 * @param editedImagePath 테두리까지 두른 최종 이미지. 화면에 띄우고 다음 단계로 넘기는 결과물이다
 * @param cutoutImagePath 테두리를 두르기 전 알맹이. 다시 편집할 때의 시작 마스크가 된다
 * @param borderLayers 두른 테두리 겹. 안쪽부터 바깥쪽 순이다
 */
data class ToppingEditResult(
    val editedImagePath: String,
    val cutoutImagePath: String,
    val borderLayers: List<ToppingBorderLayer>,
)

/**
 * 편집 화면이 결과를 돌려줄 때 쓰는 결과 키.
 *
 * 결과 타입은 [ToppingEditResult] 다. [NavKeyToppingEdit] 로 들어온 쪽이 이 키로 결과를 받는다.
 */
const val TOPPING_EDIT_RESULT_KEY = "topping_edit_result"
