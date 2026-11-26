package com.teamyg.parfait.feature.login.impl.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import com.teamyg.parfait.core.ui.gesture.HiddenTapCounter
import com.teamyg.parfait.feature.login.impl.model.CherryRect
import kotlin.math.min
import kotlin.math.roundToInt

private const val CHERRY_WIDTH_RATIO = 0.24f
private const val CHERRY_HEIGHT_RATIO = 0.28f
private const val CHERRY_CENTER_X_RATIO = 0.5f
private const val CHERRY_CENTER_Y_RATIO = 0.19f

/**
 * 온보딩 마지막 이미지의 체리 위에 얹는 투명 탭 영역. 5번째 탭에서 [onFiveTaps]를 부른다.
 *
 * 영역은 컨테이너가 아니라 `ContentScale.Fit`으로 실제 그려진 이미지 경계 기준 비율이다.
 * 드래그를 소비하지 않아야 체리 위에서 시작한 스와이프도 `HorizontalPager`가 가져간다 —
 * `detectTapGestures`는 down을 소비하므로 쓰지 않고 up만 기다린다(pager가 이동을 소비하면
 * `waitForUpOrCancellation`이 null이라 탭으로 세지 않는다).
 *
 * @param imageIntrinsicSize 이미지 원본 크기. 비율만 쓰며 알 수 없으면 컨테이너 전체를 기준으로 본다.
 */
@Composable
internal fun CherryTapArea(
    imageIntrinsicSize: Size,
    onFiveTaps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val counter = remember { HiddenTapCounter() }
    val currentOnFiveTaps by rememberUpdatedState(onFiveTaps)

    Layout(
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            if (waitForUpOrCancellation() != null && counter.tap()) {
                                currentOnFiveTaps()
                            }
                        }
                    },
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val container = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val area = cherryRect(container, imageIntrinsicSize)
        val placeable = measurables.single().measure(
            Constraints.fixed(area.width.roundToInt(), area.height.roundToInt()),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(area.left.roundToInt(), area.top.roundToInt())
        }
    }
}

/** [container] 안에 `Fit`으로 그려진 이미지 경계 위의 체리 영역 */
internal fun cherryRect(
    container: Size,
    imageIntrinsicSize: Size,
): CherryRect {
    val hasImageSize = imageIntrinsicSize.width > 0f && imageIntrinsicSize.height > 0f
    val scale = if (hasImageSize) {
        min(container.width / imageIntrinsicSize.width, container.height / imageIntrinsicSize.height)
    } else {
        null
    }
    val drawnWidth = scale?.let { imageIntrinsicSize.width * it } ?: container.width
    val drawnHeight = scale?.let { imageIntrinsicSize.height * it } ?: container.height
    val imageLeft = (container.width - drawnWidth) / 2f
    val imageTop = (container.height - drawnHeight) / 2f

    val width = drawnWidth * CHERRY_WIDTH_RATIO
    val height = drawnHeight * CHERRY_HEIGHT_RATIO
    return CherryRect(
        left = imageLeft + drawnWidth * CHERRY_CENTER_X_RATIO - width / 2f,
        top = imageTop + drawnHeight * CHERRY_CENTER_Y_RATIO - height / 2f,
        width = width,
        height = height,
    )
}
