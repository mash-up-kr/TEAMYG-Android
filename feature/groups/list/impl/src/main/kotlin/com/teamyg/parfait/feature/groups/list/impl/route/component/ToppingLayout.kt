package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun ToppingLayout(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    overlap: Dp = 12.dp,
    alternateOffsetY: Dp = 86.dp,
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

        val positions = mutableListOf<Pair<Int, Int>>()

        placeables.forEachIndexed { index, placeable ->

            if (index % 2 == 0) {
                // 왼쪽
                positions += paddingLeft to leftY

                leftY += placeable.height - overlapPx
            } else {
                // 오른쪽
                positions += (
                    constraints.maxWidth -
                        paddingRight -
                        placeable.width
                    ) to rightY

                rightY += placeable.height - overlapPx
            }
        }

        layout(
            width = constraints.maxWidth,
            height = maxOf(leftY, rightY) + paddingBottom,
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
