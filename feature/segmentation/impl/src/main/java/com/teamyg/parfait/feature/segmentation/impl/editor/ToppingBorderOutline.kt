package com.teamyg.parfait.feature.segmentation.impl.editor

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/**
 * 실루엣을 부풀릴 때 원 둘레를 몇 번 찍을지.
 *
 * 알파 마스크를 실제로 팽창시키는 대신 같은 그림을 반지름만큼 떨어진 자리에 빙 둘러 찍고,
 * 겹친 자국으로 굵은 윤곽을 만든다. 수가 적으면 윤곽에 각이 지고, 많을수록 그리기가 무거워진다.
 */
private const val OUTLINE_SAMPLE_COUNT = 24

private const val FULL_TURN_RADIANS = 2 * Math.PI

/**
 * 실루엣을 [radius] 만큼 부풀리기 위해 원본을 찍어야 할 자리들.
 *
 * 화면 미리보기와 저장이 같은 자리를 찍어야 두 결과가 어긋나지 않으므로 여기서 한 번만 계산한다.
 */
internal fun outlineOffsets(radius: Float): List<Offset> = List(OUTLINE_SAMPLE_COUNT) { index ->
    val angle = FULL_TURN_RADIANS * index / OUTLINE_SAMPLE_COUNT
    Offset(x = (cos(angle) * radius).toFloat(), y = (sin(angle) * radius).toFloat())
}

/**
 * 각 겹과 그 겹이 실루엣에서 밀려나야 할 거리를 짝지어 돌려준다.
 *
 * 겹은 아래 겹을 감싸며 쌓이므로, 밀려나는 거리는 자기 굵기가 아니라 자기까지의 굵기를 모두 더한 값이다.
 */
internal fun List<ToppingBorderStroke>.withOutsets(): List<Pair<ToppingBorderStroke, Float>> {
    var accumulated = 0f
    return map { stroke ->
        accumulated += stroke.width
        stroke to accumulated
    }
}

/** 겹을 모두 두르고 나면 실루엣 바깥으로 번지는 총 거리 */
internal fun List<ToppingBorderStroke>.totalOutset(): Float = sumOf { stroke -> stroke.width.toDouble() }.toFloat()

/**
 * 테두리까지 원본 크기 안에 담기 위해 알맹이를 줄여야 하는 비율.
 *
 * 결과 비트맵을 키우지 않기로 했으므로 번질 자리를 안쪽에서 마련한다.
 * 겹이 두꺼워질수록 알맹이가 그만큼 안으로 물러난다.
 */
internal fun List<ToppingBorderStroke>.shrinkRatio(
    bitmapWidth: Int,
    bitmapHeight: Int,
): Float {
    val outset = totalOutset()
    if (outset <= 0f) return 1f

    return minOf(
        bitmapWidth / (bitmapWidth + outset * 2),
        bitmapHeight / (bitmapHeight + outset * 2),
    )
}
