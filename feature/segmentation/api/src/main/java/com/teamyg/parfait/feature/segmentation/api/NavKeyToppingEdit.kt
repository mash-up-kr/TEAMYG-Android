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
