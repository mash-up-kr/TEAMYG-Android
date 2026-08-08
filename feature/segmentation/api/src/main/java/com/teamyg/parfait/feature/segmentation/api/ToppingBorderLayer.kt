package com.teamyg.parfait.feature.segmentation.api

import kotlinx.serialization.Serializable

/**
 * 토핑에 두른 테두리 한 겹. 안쪽부터 바깥쪽 순으로 나열한다.
 *
 * 편집 화면을 다시 열 때 두른 테두리를 그대로 되살리기 위해 화면 밖으로 나르는 형태다.
 * 편집 화면 안에서 쓰는 모델은 Compose Color 를 들고 있어 NavKey 로 실을 수 없으므로,
 * 색을 ARGB 정수로 펼쳐 둔다.
 *
 * @param colorArgb 겹의 색
 * @param width 겹의 굵기. 화면 크기가 아니라 원본 비트맵 좌표계 기준이다
 */
@Serializable
data class ToppingBorderLayer(
    val colorArgb: Int,
    val width: Float,
)
