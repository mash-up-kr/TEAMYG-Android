package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.core.util.jvm.extension.truncateToGraphemeClusters
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_MAX_LENGTH
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_MIN_SCALE
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextInputTransformStep
import com.teamyg.parfait.feature.groups.canvas.impl.model.nextTextToppingAlign
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasTextInputIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasTextInputUiState

// 그룹·혼자 공간 텍스트 입력 화면이 함께 부르는 순수 상태 계산. 저장 경로만 두 ViewModel 이 다르다

private const val CANVAS_CENTER_RATIO = 0.5f

/** 배경이 이미지면 색은 그대로 둔다 — 이미지가 없을 때만 그려지는 값이라 비우면 흰색으로 튄다 */
internal fun CanvasTextInputUiState.withCanvasContent(
    background: CanvasBackground?,
    toppings: List<CanvasToppingVO>,
    texts: List<CanvasTextToppingVO>,
): CanvasTextInputUiState = copy(
    backgroundColor = (background as? CanvasBackground.Color)
        ?.value
        ?.toColorOrNull()
        ?: backgroundColor,
    backgroundImageUrl = (background as? CanvasBackground.Image)?.url,
    existingToppings = toppings.sortedBy { topping -> topping.transform.positionZ },
    existingTexts = texts,
)

/**
 * 저장·내비게이션이 필요 없는, 화면 안 로컬 상태만 바꾸는 인텐트를 반영한다.
 * 그 밖의 인텐트는 상태를 그대로 돌려준다.
 */
internal fun CanvasTextInputUiState.withLocalIntent(intent: CanvasTextInputIntent): CanvasTextInputUiState =
    when (intent) {
        is CanvasTextInputIntent.OnContentChange ->
            copy(content = intent.value.truncateToGraphemeClusters(TEXT_TOPPING_MAX_LENGTH))

        is CanvasTextInputIntent.OnSelectColor -> copy(selectedColor = intent.color)

        CanvasTextInputIntent.OnToggleColorPopover -> copy(isColorPopoverOpen = !isColorPopoverOpen)

        CanvasTextInputIntent.OnDismissColorPopover -> copy(isColorPopoverOpen = false)

        is CanvasTextInputIntent.OnAutoFontSizeChange -> copy(designFontSize = intent.designFontSize)

        CanvasTextInputIntent.OnClickType -> copy(textType = textType.next())

        CanvasTextInputIntent.OnClickAlign -> copy(align = align.nextTextToppingAlign())

        is CanvasTextInputIntent.OnTextBoxMeasured -> copy(textBoxSize = intent.size).withInitialPlacementIfNeeded()

        is CanvasTextInputIntent.OnCanvasMeasured -> copy(canvasSize = intent.size).withInitialPlacementIfNeeded()

        CanvasTextInputIntent.OnToppingTransformGestureEnd -> copy(
            isCenterGuideXVisible = false,
            isCenterGuideYVisible = false,
            isAngleGuideVisible = false,
            hasRotatedThisGesture = false,
        )

        is CanvasTextInputIntent.OnDragPlacementSeed -> withDragPlacementSeed(intent.offsetX, intent.offsetY)

        CanvasTextInputIntent.OnClickClose -> copy(showQuitDialog = true)

        CanvasTextInputIntent.OnQuitDialogCancel -> copy(showQuitDialog = false)

        is CanvasTextInputIntent.OnToppingTransform,
        CanvasTextInputIntent.OnClickConfirm,
        CanvasTextInputIntent.OnClickStack,
        CanvasTextInputIntent.OnQuitDialogConfirm,
        -> this
    }

/** 키보드 ↑ 상태에서 끌기로 배치에 진입할 때, 점프 없이 현재 보이는 자리에서 이어지도록 배치 좌표를 심는다 */
private fun CanvasTextInputUiState.withDragPlacementSeed(
    offsetX: Dp,
    offsetY: Dp,
): CanvasTextInputUiState = copy(
    offsetX = offsetX,
    offsetY = offsetY,
    // 사용자 드래그가 아니라 좌표를 심는 지점이라 raw 도 같이 맞춘다
    rawOffsetX = offsetX,
    rawOffsetY = offsetY,
    hasUserAdjustedPlacement = true,
)

/**
 * 이동·확대·회전 한 프레임을 반영한다. 글상자·캔버스 실측이 끝났다면 중앙·각도 가이드 스냅도 건다.
 */
internal fun CanvasTextInputUiState.stepTextTransform(
    pan: DpOffset,
    zoom: Float,
    rotationDelta: Float,
): TextInputTransformStep {
    val box = textBoxSize
    val canvas = canvasSize
    if (box == null || canvas == null) {
        val next = copy(
            offsetX = offsetX + pan.x,
            offsetY = offsetY + pan.y,
            rawOffsetX = rawOffsetX + pan.x,
            rawOffsetY = rawOffsetY + pan.y,
            scale = (scale * zoom).coerceAtLeast(TEXT_TOPPING_MIN_SCALE),
            rotationDegrees = rotationDegrees + rotationDelta,
            rawRotationDegrees = rawRotationDegrees + rotationDelta,
            hasRotatedThisGesture = hasRotatedThisGesture || rotationDelta != 0f,
            hasUserAdjustedPlacement = true,
        )
        return TextInputTransformStep(state = next, justSnapped = false)
    }

    val posStep = stepCenterGuideDp(
        oldRawOffsetX = rawOffsetX,
        oldRawOffsetY = rawOffsetY,
        panX = pan.x,
        panY = pan.y,
        halfWidth = box.width / 2,
        halfHeight = box.height / 2,
        canvasCenterXDp = (canvas.width / 2).value,
        canvasCenterYDp = (canvas.height / 2).value,
    )
    val angleStep = stepAngleGuide(
        oldRawDegrees = rawRotationDegrees,
        oldHasRotated = hasRotatedThisGesture,
        rotationDelta = rotationDelta,
    )

    val next = copy(
        offsetX = posStep.guideX.effectiveValue.dp - box.width / 2,
        offsetY = posStep.guideY.effectiveValue.dp - box.height / 2,
        rawOffsetX = posStep.rawOffsetX,
        rawOffsetY = posStep.rawOffsetY,
        isCenterGuideXVisible = posStep.guideX.isSnapped,
        isCenterGuideYVisible = posStep.guideY.isSnapped,
        scale = (scale * zoom).coerceAtLeast(TEXT_TOPPING_MIN_SCALE),
        rotationDegrees = angleStep.effectiveDegrees,
        rawRotationDegrees = angleStep.rawDegrees,
        isAngleGuideVisible = angleStep.isVisible,
        isAngleGuideHorizontal = angleStep.isHorizontal,
        hasRotatedThisGesture = angleStep.hasRotated,
        hasUserAdjustedPlacement = true,
    )
    return TextInputTransformStep(state = next, justSnapped = posStep.justSnapped || angleStep.justSnapped)
}

/**
 * 사용자가 아직 손대지 않았다면, 키보드가 처음 내려갈 때 텍스트가 타이핑 중이던 자리
 * (캔버스 정중앙)에 그대로 남도록 저장 좌표를 맞춘다.
 *
 * 측정될 때마다 다시 맞춘다 — 첫 레이아웃에서는 키보드·툴바 높이가 아직 0 이라 글자 크기가
 * 거의 0 으로 재지고, 그 박스로 좌상단을 고정하면 박스가 제 크기로 커진 만큼 중심이 밀린다.
 */
private fun CanvasTextInputUiState.withInitialPlacementIfNeeded(): CanvasTextInputUiState {
    if (hasUserAdjustedPlacement) return this
    val canvas = canvasSize ?: return this
    val box = textBoxSize ?: return this

    val centeredOffsetX = canvas.width * CANVAS_CENTER_RATIO - box.width / 2
    val centeredOffsetY = canvas.height * CANVAS_CENTER_RATIO - box.height / 2

    return copy(
        offsetX = centeredOffsetX,
        offsetY = centeredOffsetY,
        rawOffsetX = centeredOffsetX,
        rawOffsetY = centeredOffsetY,
    )
}
