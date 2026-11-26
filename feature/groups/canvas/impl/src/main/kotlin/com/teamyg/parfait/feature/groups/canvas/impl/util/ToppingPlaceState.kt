package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingPlaceTransformStep
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceUiState

// 배치 화면 상태의 순수 계산. 저장·초안 접근 없이 UI 상태만 다룬다

/** 스케일링된 토핑의 짧은 변이 이보다 작아지면, 그 변을 이 크기로 맞추도록 강제 상향한다 */
private val MIN_TOPPING_SHORT_SIDE = 48.dp

/** 배경이 이미지면 색은 그대로 둔다 — 이미지가 없을 때만 그려지는 값이라 비우면 흰색으로 튄다 */
internal fun CanvasToppingPlaceUiState.withCanvasContent(
    background: CanvasBackground?,
    toppings: List<CanvasToppingVO>,
    texts: List<CanvasTextToppingVO>,
): CanvasToppingPlaceUiState = copy(
    backgroundColor = (background as? CanvasBackground.Color)
        ?.value
        ?.toColorOrNull()
        ?: backgroundColor,
    backgroundImageUrl = (background as? CanvasBackground.Image)?.url,
    existingToppings = toppings.sortedBy { topping -> topping.transform.positionZ },
    existingTexts = texts,
)

/**
 * C-106: 사용자가 아직 손대지 않았다면, 캔버스 정중앙에 토핑의 긴 변이
 * 캔버스 너비의 [TOPPING_BASE_LONG_SIDE_RATIO] 가 되도록 자동으로 놓는다.
 *
 * 캔버스 실측 크기와 토핑 원본 크기를 둘 다 알아야 계산되고, 이 둘은 서로 다른 시점에
 * 비동기로 들어오므로 어느 쪽이 먼저 도착하든 매번 다시 시도한다.
 */
internal fun CanvasToppingPlaceUiState.withInitialPlacementIfNeeded(): CanvasToppingPlaceUiState {
    if (hasUserAdjustedPlacement) return this
    val canvasSize = canvasSize ?: return this
    val baseSize = toppingBaseSize ?: return this

    val longerBaseSide = maxOf(baseSize.width, baseSize.height)
    val shorterBaseSide = minOf(baseSize.width, baseSize.height)

    val longSideScale = (canvasSize.width * TOPPING_BASE_LONG_SIDE_RATIO) / longerBaseSide
    // 위 배율대로 두면 짧은 변이 최소 터치 영역보다 작아질 수 있어, 그 경우 짧은 변 기준으로 다시 키운다
    val minTouchScale = MIN_TOPPING_SHORT_SIDE / shorterBaseSide
    val newScale = maxOf(longSideScale, minTouchScale)

    val centeredOffsetX = (canvasSize.width - baseSize.width) / 2
    val centeredOffsetY = (canvasSize.height - baseSize.height) / 2

    return copy(
        scale = newScale,
        offsetX = centeredOffsetX,
        offsetY = centeredOffsetY,
        rawOffsetX = centeredOffsetX,
        rawOffsetY = centeredOffsetY,
    )
}

/**
 * 이동·확대·회전 한 프레임을 중앙·각도 가이드 스냅과 함께 반영한다.
 *
 * @return 반영하지 않을 프레임이면 null. 실측 전에 [CanvasToppingPlaceUiState.hasUserAdjustedPlacement]
 *   가 굳으면 초기 배치가 영영 안 걸려서, 실측 전 프레임은 버린다
 */
internal fun CanvasToppingPlaceUiState.stepToppingTransform(
    pan: DpOffset,
    zoom: Float,
    rotationDelta: Float,
): ToppingPlaceTransformStep? {
    if (ignoresToppingTransform(isBorderPanelOpen)) return null
    val canvasSize = canvasSize ?: return null
    val baseSize = toppingBaseSize ?: return null

    val posStep = stepCenterGuideDp(
        oldRawOffsetX = rawOffsetX,
        oldRawOffsetY = rawOffsetY,
        panX = pan.x,
        panY = pan.y,
        halfWidth = baseSize.width / 2,
        halfHeight = baseSize.height / 2,
        canvasCenterXDp = (canvasSize.width / 2).value,
        canvasCenterYDp = (canvasSize.height / 2).value,
    )
    val angleStep = stepAngleGuide(
        oldRawDegrees = rawRotationDegrees,
        oldHasRotated = hasRotatedThisGesture,
        rotationDelta = rotationDelta,
    )

    val next = copy(
        offsetX = posStep.guideX.effectiveValue.dp - baseSize.width / 2,
        offsetY = posStep.guideY.effectiveValue.dp - baseSize.height / 2,
        rawOffsetX = posStep.rawOffsetX,
        rawOffsetY = posStep.rawOffsetY,
        isCenterGuideXVisible = posStep.guideX.isSnapped,
        isCenterGuideYVisible = posStep.guideY.isSnapped,
        scale = (scale * zoom).coerceAtLeast(minToppingPlaceScale(canvasSize, baseSize)),
        rotationDegrees = angleStep.effectiveDegrees,
        rawRotationDegrees = angleStep.rawDegrees,
        isAngleGuideVisible = angleStep.isVisible,
        isAngleGuideHorizontal = angleStep.isHorizontal,
        hasRotatedThisGesture = angleStep.hasRotated,
        hasUserAdjustedPlacement = true,
    )
    return ToppingPlaceTransformStep(state = next, justSnapped = posStep.justSnapped || angleStep.justSnapped)
}

/** 이동 제스처(드래그·핀치)가 끝났다. 가이드를 끈다 */
internal fun CanvasToppingPlaceUiState.withTransformGestureEnded(): CanvasToppingPlaceUiState = copy(
    isCenterGuideXVisible = false,
    isCenterGuideYVisible = false,
    isAngleGuideVisible = false,
    hasRotatedThisGesture = false,
)

/** [TOPPING_MIN_SCALE] 을 이 화면 배율로 환산한다. [toToppingTransform] 의 역산이다 */
private fun minToppingPlaceScale(
    canvasSize: DpSize,
    baseSize: DpSize,
): Float {
    val longerBaseSide = maxOf(baseSize.width, baseSize.height)
    return canvasSize.width * (TOPPING_MIN_SCALE * TOPPING_BASE_LONG_SIDE_RATIO) / longerBaseSide
}
