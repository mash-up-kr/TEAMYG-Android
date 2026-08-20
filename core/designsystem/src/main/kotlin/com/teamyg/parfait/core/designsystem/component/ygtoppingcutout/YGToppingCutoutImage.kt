package com.teamyg.parfait.core.designsystem.component.ygtoppingcutout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import kotlin.math.cos
import kotlin.math.sin

/** 누끼 외곽선을 찍는 방향 수. 8 방향이면 대각까지 메워져 이음매가 보이지 않는다 */
private const val OUTLINE_STAMP_COUNT = 8

private const val FULL_TURN_DEGREES = 360.0

/**
 * 누끼 이미지와 그 실루엣을 따르는 테두리를 함께 그린다. 사각 테두리를 두르면 잘라 낸 배경이 다시
 * 드러나므로, 같은 그림을 테두리 색으로 물들여 여덟 방향으로 밀어 찍고 그 위에 원본을 얹는다.
 *
 * 테두리를 그리는 화면이 셋이라 여기서 한 벌만 둔다(`adr/0025-topping-border-as-server-field.md`).
 *
 * ⚠️ **그림이 아직 뜨지 않은 [painter] 로 찍으면 플레이스홀더 실루엣이 테두리로 보인다.**
 * 준비되기 전에는 호출부가 [borderColor] 에 `null` 을 넘긴다.
 *
 * @param borderWidth 화면 기준 dp 다 — 토핑을 키워도 굵기는 그대로다
 */
@Composable
fun YGToppingCutoutImage(
    painter: Painter,
    borderColor: Color?,
    borderWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (borderColor != null) {
            ToppingOutline(
                painter = painter,
                color = borderColor,
                width = borderWidth,
            )
        }

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ToppingOutline(
    painter: Painter,
    color: Color,
    width: Dp,
) {
    val widthPx = with(LocalDensity.current) { width.toPx() }

    repeat(OUTLINE_STAMP_COUNT) { index ->
        val radians = Math.toRadians(FULL_TURN_DEGREES / OUTLINE_STAMP_COUNT * index)

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = (cos(radians) * widthPx).toFloat()
                    translationY = (sin(radians) * widthPx).toFloat()
                },
        )
    }
}

@YGPreview
@Composable
private fun YGToppingCutoutImagePreview() = PreviewBox {
    YGToppingCutoutImage(
        painter = painterResource(R.drawable.ic_plus),
        borderColor = YGAtomicColors.Cherry.Cherry200,
        borderWidth = 6.dp,
        modifier = Modifier.size(120.dp),
    )
}
