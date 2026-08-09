package com.teamyg.parfait.feature.segmentation.api

import kotlinx.serialization.Serializable

/**
 * 토핑에 두른 테두리 한 겹. 안쪽부터 바깥쪽 순으로 나열한다.
 *
 * 색상칩을 고를 때마다 그 시점의 굵기로 한 겹이 위에 쌓여 중첩되고, 되돌리기는 가장 바깥부터 벗겨낸다.
 * 편집을 마치고도 화면 밖으로 실려 나가 다시 편집할 때 그대로 되살아난다.
 *
 * 색을 Compose Color 가 아니라 ARGB 정수로 들고 있는 것은 NavKey 로 실리려면 직렬화가 되어야 하고,
 * 이 모듈이 Compose 를 알지 않아야 하기 때문이다. 편집 화면 쪽에서 Color 로 바꿔 쓴다.
 *
 * @param colorArgb 겹의 색
 * @param widthDp 겹의 굵기(dp). 붓 굵기와 같은 기준이라 사진 해상도가 달라도 화면에서 같은 굵기로 보인다.
 * 저장할 때 편집 화면이 원본 비트맵 좌표계 굵기로 환산한다
 */
@Serializable
data class ToppingBorderLayer(
    val colorArgb: Int,
    val widthDp: Float,
)
