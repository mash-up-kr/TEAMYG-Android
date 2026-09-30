package com.teamyg.parfait.feature.segmentation.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Segmentation 결과를 토핑으로 쓰기 전에 손으로 다듬는 편집 화면의 진입점.
 *
 * @param sourceImageUri 원본 이미지. 제거했던 영역을 다시 채울 때 이 픽셀을 가져온다
 * @param segmentationImageUri Segmentation 으로 잘라낸 이미지. 이 이미지의 알파가 편집의 시작 마스크가 된다
 * @param borderLayers 이미 두른 테두리 겹. 다시 편집할 때 벗겨진 채로 열리지 않도록 되살릴 재료다
 * @param borderOnly 되살릴 원본이 없는 진입은 영역(잘라내기)을 건드릴 수 없고 테두리만 고칠 수
 * 있어야 한다. true 면 영역|테두리 탭 전환 없이 테두리 편집만 열린다
 * @param completion 편집을 마쳤을 때 결과를 어떻게 처리할지
 */
@Serializable
data class NavKeyToppingEdit(
    val sourceImageUri: String,
    val segmentationImageUri: String,
    val borderLayers: List<ToppingBorderLayer> = emptyList(),
    val borderOnly: Boolean = false,
    val completion: ToppingEditCompletion = ToppingEditCompletion.ReturnResult,
) : NavKey

/** 편집 화면이 완료 시 결과를 처리하는 방식 */
@Serializable
enum class ToppingEditCompletion {
    /** 결과를 [TOPPING_EDIT_RESULT_KEY] 로 돌려주고 닫는다. 확인 화면 "사진 편집", 배경 편집 */
    ReturnResult,

    /** 초안을 직접 기록하고 확인 화면으로 간다. 편집 화면은 백스택에 남는다. 0개 경로 */
    RecordAndConfirm,
}

/**
 * 편집 결과.
 *
 * 테두리는 픽셀에 굽지 않고 값으로 나른다(`adr/0025-topping-border-as-server-field.md`).
 * 그리는 것은 결과를 받는 쪽이다.
 *
 * @param subjectImagePath 테두리를 두르지 않은 알맹이. 투명 여백을 걷어 실제 토핑 크기다
 * @param cutoutImagePath 다시 편집할 때의 시작 마스크. 원본 좌표계를 지켜야 해 여백을 걷지 않는다
 * @param sourceLongSide 원본 사진 전체의 긴 변(px). 원본 사진이 남아 있지 않은 진입에서는 null 이다.
 * 이 모듈은 `domain`을 의존하지 않으므로 `SourceLongSide` 값 타입이 아니라 벌거벗은 `Int`로 나른다
 * (근거는 `adr/0002-feature-api-impl-split.md`)
 */
data class ToppingEditResult(
    val subjectImagePath: String,
    val cutoutImagePath: String,
    val borderLayers: List<ToppingBorderLayer>,
    val sourceLongSide: Int?,
)

/**
 * 편집 화면이 결과를 돌려줄 때 쓰는 결과 키.
 *
 * 결과 타입은 [ToppingEditResult] 다. [NavKeyToppingEdit] 로 들어온 쪽이 이 키로 결과를 받는다.
 */
const val TOPPING_EDIT_RESULT_KEY = "topping_edit_result"
