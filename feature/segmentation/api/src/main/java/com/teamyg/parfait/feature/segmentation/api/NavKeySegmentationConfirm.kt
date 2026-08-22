package com.teamyg.parfait.feature.segmentation.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 분리된 객체 이미지를 확인하는 화면.
 *
 * 최근 목록에서 되살린 알맹이로 들어오면 앞의 둘이 없다. 그때는 "사진 편집"이 잠긴다.
 *
 * @param sourceImageUri 원본 이미지. 여기서 수동 편집으로 넘어갈 때 지운 영역을 되살릴 재료로 쓴다
 * @param subjectImagePath 배경이 제거된 객체 이미지의 파일 경로
 * @param trimmedSubjectImagePath 위 이미지에서 투명한 여백을 걷어내 객체 크기만 남긴 파일 경로.
 */
@Serializable
data class NavKeySegmentationConfirm(
    val sourceImageUri: String?,
    val subjectImagePath: String?,
    val trimmedSubjectImagePath: String,
) : NavKey
