package com.teamyg.parfait.core.designsystem.component.ygskeleton

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

const val YG_SKELETON_TEST_TAG = "yg_skeleton"

private const val SHIMMER_DURATION_MILLIS = 1_200
private const val BAND_WIDTH_RATIO = 0.4f

/**
 * 원격 이미지를 기다리는 동안 그 자리를 대신 채우는 회색 면. 밝은 띠가 가로로
 * 흐르며 아직 오는 중이라는 것을 알린다.
 *
 * **자리를 아는 곳에만 쓴다.** 크기를 [modifier] 로 받으므로, 그릴 상자의 크기가
 * 이미지가 도착해야 정해지는 자리에 쓰면 로딩이 끝나는 순간 크기가 튄다.
 *
 * 색과 주기는 디자인 확정 전이라 바뀌면 이 파일만 고친다.
 *
 * 흐르는 값은 [drawBehind] 안에서 읽는다 — 컴포지션에서 읽으면 프레임마다 재구성이
 * 돈다. 그리기 단계에서만 읽어 무효화를 draw 로 좁힌다.
 */
@Composable
fun YGSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
) {
    val transition = rememberInfiniteTransition(label = "YGSkeleton")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "YGSkeletonShimmer",
    )

    Box(
        modifier = modifier
            .testTag(YG_SKELETON_TEST_TAG)
            .clip(shape)
            .drawBehind {
                drawRect(color = YGAtomicColors.Gray.Gray100)

                val bandWidth = size.width * BAND_WIDTH_RATIO
                // 띠가 왼쪽 바깥에서 들어와 오른쪽 바깥으로 빠져나가는 구간을 progress 로 훑는다
                val bandStart = -bandWidth + (size.width + bandWidth) * progress.value

                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, YGAtomicColors.Gray.White, Color.Transparent),
                        start = Offset(x = bandStart, y = 0f),
                        end = Offset(x = bandStart + bandWidth, y = size.height),
                    ),
                )
            },
    )
}

@YGPreview
@Composable
private fun YGSkeletonPreview() = PreviewBox {
    YGSkeleton(modifier = Modifier.size(SizeTokens.Size96.getDp()))
}
