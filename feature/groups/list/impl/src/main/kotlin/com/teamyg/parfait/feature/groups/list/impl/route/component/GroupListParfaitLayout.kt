package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.feature.groups.list.impl.R

/**
 * 파르페를 cherry - top - middle* - bottom 순서로 가운데 정렬해 세로로 쌓는 Layout.
 *
 * content 는 cherrySection 과 같은 y=0 에서 시작해 쌓인 파르페 위에 겹쳐서 그려진다.
 * 기본적으로 middleSection 을 1개만 배치하지만, content 의 하단이 마지막 middleSection 의 하단보다
 * 아래로 내려가면 다 덮을 때까지 middleSection 을 반복해서 추가한다.
 * bottomSection 은 토핑을 받치지 않으므로 이 비교에 포함하지 않는다.
 *
 * @param cherryOverlap cherrySection 과 topSection 이 세로로 겹치는 양
 * @param topOverlap topSection 과 첫 middleSection 이 세로로 겹치는 양
 * @param middleOverlap middleSection 끼리 세로로 겹치는 양
 * @param bottomOverlap 마지막 middleSection 과 bottomSection 이 세로로 겹치는 양
 */
@Composable
internal fun GroupListParfaitLayout(
    cherrySection: @Composable () -> Unit,
    topSection: @Composable () -> Unit,
    middleSection: @Composable () -> Unit,
    bottomSection: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    cherryOverlap: Dp = GroupListParfaitDefaults.CherryOverlap,
    topOverlap: Dp = GroupListParfaitDefaults.TopOverlap,
    middleOverlap: Dp = GroupListParfaitDefaults.MiddleOverlap,
    bottomOverlap: Dp = GroupListParfaitDefaults.BottomOverlap,
    content: @Composable () -> Unit = {},
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val sectionConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val contentPlaceables = measureSection(ParfaitSlot.Content, sectionConstraints, content)
        val cherryPlaceables = measureSection(ParfaitSlot.Cherry, sectionConstraints, cherrySection)
        val topPlaceables = measureSection(ParfaitSlot.Top, sectionConstraints, topSection)
        val bottomPlaceables = measureSection(ParfaitSlot.Bottom, sectionConstraints, bottomSection)
        val firstMiddlePlaceables = measureSection(ParfaitSlot.Middle(0), sectionConstraints, middleSection)

        val contentHeight = contentPlaceables.maxHeight()
        val cherryHeight = cherryPlaceables.maxHeight()
        val topHeight = topPlaceables.maxHeight()
        val middleHeight = firstMiddlePlaceables.maxHeight()
        val bottomHeight = bottomPlaceables.maxHeight()

        val cherryOverlapPx = cherryOverlap.roundToPx()
        val topOverlapPx = topOverlap.roundToPx()
        val middleOverlapPx = middleOverlap.roundToPx()
        val bottomOverlapPx = bottomOverlap.roundToPx()

        // 첫 middle 이 시작하는 y. content 는 y=0 에서 시작하므로 이 offset 을 더해야 같은 원점에서 비교할 수 있다
        val creamTop = (cherryHeight - cherryOverlapPx) + (topHeight - topOverlapPx)

        // middle 1개일 때 크림이 덮는 하단. bottomSection 은 토핑을 받치지 않으므로 여기서 제외한다
        val coveredHeight = creamTop + middleHeight

        // middle 을 하나 더 쌓을 때마다 실제로 늘어나는 높이. 0 이하면 아무리 쌓아도 높이가 늘지 않으므로 반복하지 않는다
        val middleStep = middleHeight - middleOverlapPx
        val middleCount = if (middleStep > 0 && contentHeight > coveredHeight) {
            // 모자란 높이를 middleStep 으로 올림 나눗셈해서 추가로 필요한 middle 개수를 구한다
            1 + ((contentHeight - coveredHeight + middleStep - 1) / middleStep)
        } else {
            1
        }

        val middlePlaceables = List(middleCount) { index ->
            if (index == 0) {
                firstMiddlePlaceables
            } else {
                measureSection(ParfaitSlot.Middle(index), sectionConstraints, middleSection)
            }
        }

        // middle 1개짜리 전체 스택 높이
        val sectionHeightSum = cherryHeight + topHeight + middleHeight + bottomHeight
        val fixedOverlapSum = cherryOverlapPx + topOverlapPx + bottomOverlapPx
        val baseHeight = sectionHeightSum - fixedOverlapSum

        val sectionsHeight = baseHeight + ((middleCount - 1) * middleStep)
        val layoutWidth = maxOf(
            contentPlaceables.maxWidth(),
            cherryPlaceables.maxWidth(),
            topPlaceables.maxWidth(),
            firstMiddlePlaceables.maxWidth(),
            bottomPlaceables.maxWidth(),
        ).coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = maxOf(sectionsHeight, contentHeight)
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width = layoutWidth, height = layoutHeight) {
            var y = 0

            placeCentered(cherryPlaceables, layoutWidth, y, ParfaitZIndex.FRONT)
            y += cherryHeight - cherryOverlapPx

            placeCentered(topPlaceables, layoutWidth, y, ParfaitZIndex.FRONT)
            y += topHeight - topOverlapPx

            // 뒤에 쌓이는 middle 일수록 앞선 middle 뒤로 그려지도록 zIndex 를 낮춘다
            middlePlaceables.forEachIndexed { index, placeables ->
                placeCentered(placeables, layoutWidth, y, ParfaitZIndex.FRONT - (index + 1).toFloat())
                y += middleHeight - middleOverlapPx
            }
            // 마지막 middle 은 middleOverlap 이 아니라 bottomOverlap 만큼만 bottom 과 겹친다
            y += middleOverlapPx - bottomOverlapPx

            placeCentered(bottomPlaceables, layoutWidth, y, ParfaitZIndex.FRONT)
            placeCentered(contentPlaceables, layoutWidth, 0, ParfaitZIndex.CONTENT)
        }
    }
}

internal object GroupListParfaitDefaults {
    val CherryOverlap: Dp = 40.dp
    val TopOverlap: Dp = 44.dp
    val MiddleOverlap: Dp = 66.dp
    val BottomOverlap: Dp = 32.dp
}

private object ParfaitZIndex {
    const val FRONT = 0f
    const val CONTENT = 1f
}

private sealed interface ParfaitSlot {
    data object Cherry : ParfaitSlot

    data object Top : ParfaitSlot

    data object Bottom : ParfaitSlot

    data object Content : ParfaitSlot

    data class Middle(val index: Int) : ParfaitSlot
}

private fun SubcomposeMeasureScope.measureSection(
    slotId: ParfaitSlot,
    constraints: Constraints,
    section: @Composable () -> Unit,
): List<Placeable> = subcompose(slotId, section).map { it.measure(constraints) }

private fun List<Placeable>.maxWidth(): Int = maxOfOrNull { it.width } ?: 0

private fun List<Placeable>.maxHeight(): Int = maxOfOrNull { it.height } ?: 0

private fun Placeable.PlacementScope.placeCentered(
    placeables: List<Placeable>,
    layoutWidth: Int,
    y: Int,
    zIndex: Float,
) = placeables.forEach { placeable ->
    placeable.placeRelative(
        x = (layoutWidth - placeable.width) / 2,
        y = y,
        zIndex = zIndex,
    )
}

@Preview
@Composable
private fun GroupListParfaitLayoutShortPreview() {
    GroupListParfaitLayout(
        cherrySection = {
            Image(
                painter = painterResource(R.drawable.parfait_cherry),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(83.dp),
            )
        },
        topSection = {
            Image(
                painter = painterResource(R.drawable.parfait_cream_top),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(210.dp),
            )
        },
        middleSection = {
            Image(
                painter = painterResource(R.drawable.parfait_cream_default),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(241.dp),
            )
        },
        bottomSection = {
            Image(
                painter = painterResource(R.drawable.parfait_cup),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(324.dp),
            )
        },
    ) {
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Preview
@Composable
private fun GroupListParfaitLayoutLongPreview() {
    GroupListParfaitLayout(
        cherrySection = {
            Image(
                painter = painterResource(R.drawable.parfait_cherry),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(83.dp),
            )
        },
        topSection = {
            Image(
                painter = painterResource(R.drawable.parfait_cream_top),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(210.dp),
            )
        },
        middleSection = {
            Image(
                painter = painterResource(R.drawable.parfait_cream_default),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(241.dp),
            )
        },
        bottomSection = {
            Image(
                painter = painterResource(R.drawable.parfait_cup),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(324.dp),
            )
        },
    ) {
        Spacer(modifier = Modifier.height(800.dp))
    }
}
