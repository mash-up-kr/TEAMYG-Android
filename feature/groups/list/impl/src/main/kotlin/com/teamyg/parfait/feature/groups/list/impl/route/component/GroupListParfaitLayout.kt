package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 파르페를 cherry - top - middle* - bottom 순서로 가운데 정렬해 세로로 쌓는 Layout.
 *
 * content 는 cherrySection 과 같은 y=0 에서 시작해 쌓인 파르페 위에 겹쳐서 그려진다.
 * 기본적으로 middleSection 을 1개만 배치하지만, content 의 하단이 마지막 middleSection 의 하단보다
 * 아래로 내려가면 다 덮을 때까지 middleSection 을 반복해서 추가한다.
 * bottomSection 은 토핑을 받치지 않으므로 이 비교에 포함하지 않는다.
 *
 * middleSection 개수가 바뀌면 [animationSpec] 에 따라 애니메이션한다. 늘어날 때는 직전 middleSection
 * 자리에서 fadeIn 하며 미끄러져 내려오고, 줄어들 때는 제자리에서 fadeOut 만 한다.
 * bottomSection 은 양쪽 모두 연속적으로 따라 움직인다.
 *
 * @param cherryOverlap cherrySection 과 topSection 이 세로로 겹치는 양
 * @param topOverlap topSection 과 첫 middleSection 이 세로로 겹치는 양
 * @param middleOverlap middleSection 끼리 세로로 겹치는 양
 * @param bottomOverlap 마지막 middleSection 과 bottomSection 이 세로로 겹치는 양
 * @param animationSpec middleSection 이 늘어나거나 줄어들 때 쓰는 스펙. 개수의 소수부가 알파와 위치를
 * 동시에 결정하므로 오버슈트하는 스펙을 넘기면 안 된다
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
    animationSpec: AnimationSpec<Float> = GroupListParfaitDefaults.MiddleAnimationSpec,
    content: @Composable () -> Unit = {},
) {
    // measure 에서 계산한 목표 개수. 값이 바뀔 때만 써서 무효화 루프를 막는다
    var requiredMiddleCount by remember { mutableIntStateOf(NOT_MEASURED) }
    var hasSnapped by remember { mutableStateOf(false) }
    val animatedMiddleCount = remember { Animatable(MIN_MIDDLE_COUNT.toFloat()) }

    LaunchedEffect(requiredMiddleCount) {
        // 첫 measure 전에는 목표 개수를 모르므로 아무것도 하지 않는다. 여기서 스냅해버리면
        // 뒤이어 들어오는 실제 개수로 애니메이션이 돌아 진입할 때마다 파르페가 자라 보인다
        if (requiredMiddleCount == NOT_MEASURED) return@LaunchedEffect

        val target = requiredMiddleCount.toFloat()
        if (hasSnapped) {
            animatedMiddleCount.animateTo(target, animationSpec)
        } else {
            // 화면에 처음 그려질 때는 애니메이션 없이 목표 개수로 바로 맞춘다
            hasSnapped = true
            animatedMiddleCount.snapTo(target)
        }
    }

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
        val required = if (middleStep > 0 && contentHeight > coveredHeight) {
            // 모자란 높이를 middleStep 으로 올림 나눗셈해서 추가로 필요한 middle 개수를 구한다
            1 + ((contentHeight - coveredHeight + middleStep - 1) / middleStep)
        } else {
            MIN_MIDDLE_COUNT
        }
        if (required != requiredMiddleCount) {
            requiredMiddleCount = required
        }

        // 애니메이션 중에는 소수를 가진다. 이 값 하나가 middle 개수, 위치, 알파, bottom 위치를 모두 결정한다
        val stackCount = if (hasSnapped) animatedMiddleCount.value else required.toFloat()
        // 사라지는 middle 도 알파가 0 이 될 때까지는 살려둔다
        val composedMiddleCount = ceil(stackCount).toInt().coerceAtLeast(MIN_MIDDLE_COUNT)
        val isShrinking = stackCount > required

        val middlePlaceables = List(composedMiddleCount) { index ->
            if (index == 0) {
                firstMiddlePlaceables
            } else {
                measureSection(ParfaitSlot.Middle(index), sectionConstraints, middleSection)
            }
        }

        val lastMiddleTop = creamTop + ((stackCount - 1f) * middleStep).roundToInt()
        // 마지막 middle 은 middleOverlap 이 아니라 bottomOverlap 만큼만 bottom 과 겹친다
        val bottomTop = lastMiddleTop + middleHeight - bottomOverlapPx
        val sectionsHeight = bottomTop + bottomHeight
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
            placeCentered(cherryPlaceables, layoutWidth, 0, ParfaitZIndex.FRONT)
            placeCentered(topPlaceables, layoutWidth, cherryHeight - cherryOverlapPx, ParfaitZIndex.FRONT)

            middlePlaceables.forEachIndexed { index, placeables ->
                // 늘어날 때는 직전 middle 자리에서 미끄러져 내려오고, 줄어들 때는 제자리에서 알파만 빠진다
                val slot = if (isShrinking) index.toFloat() else minOf(index.toFloat(), stackCount - 1f)
                placeCentered(
                    placeables = placeables,
                    layoutWidth = layoutWidth,
                    y = creamTop + (slot * middleStep).roundToInt(),
                    // 뒤에 쌓이는 middle 일수록 앞선 middle 뒤로 그려지도록 zIndex 를 낮춘다
                    zIndex = ParfaitZIndex.FRONT - (index + 1).toFloat(),
                    // 위치보다 알파를 빠르게 채워야 마지막 middle 이 반투명한 동안 배경이 비치지 않는다
                    alpha = ((stackCount - index) * ALPHA_RAMP).coerceIn(0f, 1f),
                )
            }

            placeCentered(bottomPlaceables, layoutWidth, bottomTop, ParfaitZIndex.FRONT)
            placeCentered(contentPlaceables, layoutWidth, 0, ParfaitZIndex.CONTENT)
        }
    }
}

private const val MIN_MIDDLE_COUNT = 2

// 아직 measure 가 한 번도 돌지 않았음을 뜻하는 값
private const val NOT_MEASURED = 0

// 알파가 위치보다 이만큼 빨리 차오른다. 1 이면 전환 중에 크림과 bottom 사이가 잠깐 비친다
private const val ALPHA_RAMP = 2f

internal object GroupListParfaitDefaults {
    val CherryOverlap: Dp = 40.dp
    val TopOverlap: Dp = 44.dp
    val MiddleOverlap: Dp = 66.dp
    val BottomOverlap: Dp = 32.dp

    val MiddleAnimationSpec: AnimationSpec<Float> = tween(
        durationMillis = 400,
        easing = FastOutSlowInEasing,
    )
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

/**
 * 알파를 레이어로 넘기기 때문에 애니메이션 중 위치·알파 변화가 recomposition 이나 redraw 없이 처리된다.
 */
private fun Placeable.PlacementScope.placeCentered(
    placeables: List<Placeable>,
    layoutWidth: Int,
    y: Int,
    zIndex: Float,
    alpha: Float = 1f,
) = placeables.forEach { placeable ->
    placeable.placeRelativeWithLayer(
        x = (layoutWidth - placeable.width) / 2,
        y = y,
        zIndex = zIndex,
    ) {
        this.alpha = alpha
    }
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
