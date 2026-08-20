package com.teamyg.parfait.feature.segmentation.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Segmentation 결과를 토핑으로 쓰기 전에 손으로 다듬는 편집 화면의 진입점.
 *
 * @param sourceImageUri 원본 이미지. 제거했던 영역을 다시 채울 때 이 픽셀을 가져온다
 * @param segmentationImageUri Segmentation 으로 잘라낸 이미지. 이 이미지의 알파가 편집의 시작 마스크가 된다
 * @param borderLayers 이미 두른 테두리 겹. 다시 편집할 때 벗겨진 채로 열리지 않도록 되살릴 재료다
 * @param borderOnly 이미 캔버스에 놓인 토핑을 다시 손볼 때는 영역(잘라내기)은 건드릴 수 없고 테두리만
 * 고칠 수 있어야 한다. true 면 영역|테두리 탭 전환 없이 테두리 편집만 열린다
 */
@Serializable
data class NavKeyToppingEdit(
    val sourceImageUri: String,
    val segmentationImageUri: String,
    val borderLayers: List<ToppingBorderLayer> = emptyList(),
    val borderOnly: Boolean = false,
) : NavKey

/**
 * 편집 결과.
 *
 * 테두리는 픽셀에 굽지 않고 값으로 나른다(`adr/0025-topping-border-as-server-field.md`).
 * 그리는 것은 결과를 받는 쪽이다.
 *
 * @param subjectImagePath 테두리를 두르지 않은 알맹이. 투명 여백을 걷어 실제 토핑 크기다
 * @param cutoutImagePath 다시 편집할 때의 시작 마스크. 원본 좌표계를 지켜야 해 여백을 걷지 않는다
 */
data class ToppingEditResult(
    val subjectImagePath: String,
    val cutoutImagePath: String,
    val borderLayers: List<ToppingBorderLayer>,
)

/**
 * 편집 화면이 결과를 돌려줄 때 쓰는 결과 키.
 *
 * 결과 타입은 [ToppingEditResult] 다. [NavKeyToppingEdit] 로 들어온 쪽이 이 키로 결과를 받는다.
 */
const val TOPPING_EDIT_RESULT_KEY = "topping_edit_result"
