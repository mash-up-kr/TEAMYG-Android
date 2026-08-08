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

/** 화면 미리보기와 저장이 같은 자리를 찍어야 두 결과가 어긋나지 않으므로 여기서 한 번만 계산한다 */
internal fun outlineOffsets(radius: Float): List<Offset> = List(OUTLINE_SAMPLE_COUNT) { index ->
    val angle = FULL_TURN_RADIANS * index / OUTLINE_SAMPLE_COUNT
    Offset(x = (cos(angle) * radius).toFloat(), y = (sin(angle) * radius).toFloat())
}

/** 겹은 아래 겹을 감싸며 쌓이므로, 밀려나는 거리는 자기 굵기가 아니라 자기까지의 굵기를 모두 더한 값이다 */
internal fun List<ToppingBorderLayer>.withOutsets(): List<Pair<ToppingBorderLayer, Float>> {
    var accumulated = 0f
    return map { layer ->
        accumulated += layer.width
        layer to accumulated
    }
}
