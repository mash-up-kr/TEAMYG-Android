package com.teamyg.parfait.feature.segmentation.impl.editor

import androidx.compose.ui.geometry.Offset
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
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
 * 반지름 1 일 때 찍을 자리. 반지름을 곱하면 실제 자리가 나온다.
 *
 * 화면은 프레임마다 이 자리를 다시 훑으므로 반지름마다 목록을 새로 만들지 않도록 한 번만 계산해 둔다.
 * 미리보기와 저장이 같은 목록을 보기도 해야 두 결과가 어긋나지 않는다.
 */
private val OUTLINE_UNIT_OFFSETS: List<Offset> = List(OUTLINE_SAMPLE_COUNT) { index ->
    val angle = FULL_TURN_RADIANS * index / OUTLINE_SAMPLE_COUNT
    Offset(x = cos(angle).toFloat(), y = sin(angle).toFloat())
}

/** [radius] 만큼 떨어진 자리를 빙 둘러 훑는다 */
internal inline fun forEachOutlineOffset(
    radius: Float,
    action: (Offset) -> Unit,
) {
    OUTLINE_UNIT_OFFSETS.forEach { unit -> action(unit * radius) }
}

/**
 * 겹을 가장 바깥부터 훑으며 저마다 밀려날 거리를 함께 넘긴다.
 *
 * 겹은 아래 겹을 감싸며 쌓이므로 밀려나는 거리는 자기 굵기가 아니라 자기까지의 굵기를 모두 더한 값이다.
 * 그릴 때는 가장 바깥부터 깔아야 안쪽 겹이 위에 남으므로 훑는 방향도 바깥에서 안쪽이다.
 *
 * 굵기는 dp 로 들고 있으므로 [pxPerDp] 를 곱해 그리는 쪽 좌표계로 환산해 넘긴다.
 * 미리보기는 화면 px 로, 저장은 원본 비트맵 px 로 환산하는 값을 각각 넘긴다.
 */
internal inline fun List<ToppingBorderLayer>.forEachOutsetOutermostFirst(
    pxPerDp: Float,
    action: (ToppingBorderLayer, Float) -> Unit,
) {
    var outsetDp = 0f
    forEach { layer -> outsetDp += layer.widthDp }

    for (index in lastIndex downTo 0) {
        val layer = this[index]
        action(layer, outsetDp * pxPerDp)
        outsetDp -= layer.widthDp
    }
}
