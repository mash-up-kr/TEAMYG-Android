package com.teamyg.parfait.feature.segmentation.impl.editor

import com.teamyg.parfait.core.util.jvm.model.ToppingBorderBand
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer

/**
 * 겹을 구간으로 펴 놓는다.
 *
 * 겹은 아래 겹을 감싸며 쌓이므로 바깥 끝은 자기 굵기가 아니라 자기까지의 굵기를 모두 더한 값이다.
 *
 * 굵기는 dp 로 들고 있으므로 [pxPerDp] 를 곱해 그리는 쪽 좌표계로 환산한다.
 */
internal fun List<ToppingBorderLayer>.toBorderBands(pxPerDp: Float): List<ToppingBorderBand> {
    var outsetDp = 0f
    return map { layer ->
        outsetDp += layer.widthDp
        ToppingBorderBand(outsetPx = outsetDp * pxPerDp, colorArgb = layer.colorArgb)
    }
}
