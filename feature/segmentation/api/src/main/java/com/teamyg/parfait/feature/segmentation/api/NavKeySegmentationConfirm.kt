package com.teamyg.parfait.feature.segmentation.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 분리된 객체 이미지를 확인하는 화면.
 *
 * @param subjectImagePath 배경이 제거된 객체 이미지의 파일 경로
 */
@Serializable
data class NavKeySegmentationConfirm(val subjectImagePath: String) : NavKey
