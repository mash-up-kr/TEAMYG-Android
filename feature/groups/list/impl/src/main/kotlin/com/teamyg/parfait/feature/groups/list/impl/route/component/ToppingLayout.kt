package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import com.teamyg.parfait.core.ui.reveal.isStaggerRevealed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlinx.coroutines.launch

@Composable
fun ToppingLayout(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    overlap: Dp = 12.dp,
    alternateOffsetY: Dp = 86.dp,
    revealedCount: Int = Int.MAX_VALUE,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current

    val paddingLeft = with(density) {
        contentPadding.calculateLeftPadding(LayoutDirection.Ltr).roundToPx()
    }

    val paddingRight = with(density) {
        contentPadding.calculateRightPadding(LayoutDirection.Ltr).roundToPx()
    }

    val paddingTop = with(density) {
        contentPadding.calculateTopPadding().roundToPx()
    }

    val paddingBottom = with(density) {
        contentPadding.calculateBottomPadding().roundToPx()
    }

    val overlapPx = with(density) {
        overlap.roundToPx()
    }

    val alternateOffsetYPx = with(density) {
        alternateOffsetY.roundToPx()
    }

    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->

        val placeables = measurables.map {
            it.measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                ),
            )
        }

        // 시작 위치 차이 적용
        var leftY = paddingTop
        var rightY = paddingTop + alternateOffsetYPx

        // 마지막 아이템에도 overlap 이 빠진 leftY/rightY 대신, 실제로 놓인 아이템의 하단 중 가장 아래를 높이로 쓴다.
        //
        // 아직 안 드러난 아이템은 높이에서만 뺀다. 파르페가 이 높이로 크림 개수를 정하므로
        // 높이가 한 칸씩 자라야 크림도 따라 붙고, 측정·배치를 그대로 둬야 이미지 요청이 이어진다
        var contentBottom = paddingTop

        val positions = mutableListOf<Pair<Int, Int>>()

        placeables.forEachIndexed { index, placeable ->

            if (index % 2 == 0) {
                // 왼쪽
                positions += paddingLeft to leftY

                if (isStaggerRevealed(index = index, revealedCount = revealedCount)) {
                    contentBottom = maxOf(contentBottom, leftY + placeable.height)
                }
                leftY += placeable.height - overlapPx
            } else {
                // 오른쪽
                positions += (
                    constraints.maxWidth -
                        paddingRight -
                        placeable.width
                    ) to rightY

                if (isStaggerRevealed(index = index, revealedCount = revealedCount)) {
                    contentBottom = maxOf(contentBottom, rightY + placeable.height)
                }
                rightY += placeable.height - overlapPx
            }
        }

        layout(
            width = constraints.maxWidth,
            height = contentBottom + paddingBottom,
        ) {
            placeables.forEachIndexed { index, placeable ->

                val (x, y) = positions[index]

                placeable.place(
                    x = x,
                    y = y,
                )
            }
        }
    }
}

/**
 * 목록 중간에 항목이 추가되거나 빠져서 자리가 밀릴 때, 새 자리로 순간이동하는 대신 애니메이션으로 이동시킨다.
 *
 * [ToppingLayout] 은 index 로 좌우와 y 를 정하므로 앞에 하나만 추가돼도 뒤 항목이 전부 반대편으로 옮겨간다.
 * 항목이 이동한 것으로 인식되려면 호출부에서 각 항목을 안정적인 key 로 감싸야 한다.
 */
@Composable
fun Modifier.animateToppingPlacement(
    animationSpec: AnimationSpec<IntOffset> = ToppingLayoutDefaults.PlacementAnimationSpec,
): Modifier {
    val scope = rememberCoroutineScope()
    // 아직 한 번도 배치되지 않았으면 null. 처음 자리를 잡을 때는 애니메이션 없이 그대로 둬야 한다
    var targetOffset by remember { mutableStateOf<IntOffset?>(null) }
    var animatable by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }

    // onPlaced 를 offset 바깥에 둬야 offset 이 반영되기 전의, 부모가 정해준 원래 자리를 읽는다
    return this
        .onPlaced { targetOffset = it.positionInParent().round() }
        .offset {
            val target = targetOffset
            if (target == null) {
                IntOffset.Zero
            } else {
                val current = animatable
                    ?: Animatable(target, IntOffset.VectorConverter).also { animatable = it }
                if (current.targetValue != target) {
                    scope.launch { current.animateTo(target, animationSpec) }
                }
                // 부모가 이미 새 자리에 배치했으므로, 아직 못 따라온 만큼을 되돌려 그린다
                current.value - target
            }
        }
}

object ToppingLayoutDefaults {
    val PlacementAnimationSpec: AnimationSpec<IntOffset> = tween(
        durationMillis = 400,
        easing = FastOutSlowInEasing,
    )
}
