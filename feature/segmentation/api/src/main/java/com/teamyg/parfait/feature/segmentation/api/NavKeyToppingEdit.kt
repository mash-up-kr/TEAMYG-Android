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
 * 편집 화면이 편집본 경로를 돌려줄 때 쓰는 결과 키.
 *
 * 결과 타입은 [String] 이며, 저장된 편집본의 파일 경로다.
 * [NavKeyToppingEdit] 로 들어온 쪽이 이 키로 결과를 받는다.
 */
const val TOPPING_EDIT_RESULT_KEY = "topping_edit_result"
