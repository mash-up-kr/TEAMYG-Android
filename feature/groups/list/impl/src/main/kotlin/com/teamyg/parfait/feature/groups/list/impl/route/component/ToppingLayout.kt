package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.teamyg.parfait.core.ui.reveal.RevealState
import androidx.compose.ui.unit.dp

@Composable
fun ToppingLayout(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    overlap: Dp = 12.dp,
    alternateOffsetY: Dp = 86.dp,
    reveal: RevealState = RevealState.AllRevealed,
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

                if (reveal.isRevealed(index)) {
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

                if (reveal.isRevealed(index)) {
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
