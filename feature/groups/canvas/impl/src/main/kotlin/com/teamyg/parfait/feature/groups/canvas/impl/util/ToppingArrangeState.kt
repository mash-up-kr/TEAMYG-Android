package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.TextToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasToppingKey
import com.teamyg.parfait.feature.groups.canvas.impl.model.CenterGuideStep
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableText
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_MIN_SCALE
import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingArrangeChanges
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingArrangeIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingArrangeUiState
import kotlin.math.abs

// 그룹·혼자 공간 재편집 화면이 함께 부르는 순수 상태 계산. 저장 경로만 두 ViewModel 이 다르다

/** 손대지 않은 축이 float 오차만으로 바뀐 것으로 잡히지 않을 만큼만 둔다 */
private const val TEXT_TRANSFORM_TOLERANCE = 1e-4f

/** 이 화면은 배경을 고르지 않으므로 방출마다 저장된 값을 따른다 */
internal fun CanvasToppingArrangeUiState.withCanvasBackground(
    background: CanvasBackground?,
): CanvasToppingArrangeUiState = copy(
    backgroundColor = (background as? CanvasBackground.Color)
        ?.value
        ?.toColorOrNull()
        ?: backgroundColor,
    backgroundImageUrl = (background as? CanvasBackground.Image)?.url,
)

/** 포커스는 최초 방출에만 시딩한다 — 이후 방출이 사용자의 포커스를 덮으면 안 된다 */
internal fun CanvasToppingArrangeUiState.withInitialFocusIfNeeded(
    hasSeeded: Boolean,
    initialToppingId: Long,
    initialIsText: Boolean,
): CanvasToppingArrangeUiState = copy(
    focusedKey = when {
        hasSeeded -> focusedKey
        initialIsText -> CanvasToppingKey.Text(initialToppingId)
        else -> CanvasToppingKey.Image(initialToppingId)
    },
)

/**
 * 최초 방출도 예외가 아니다 — 그때는 두 집합이 비어 있어 결과가 통째 대입과 같아진다.
 * 화면은 그 방출이 어디서 왔는지 구분하지 않는다.
 */
internal fun CanvasToppingArrangeUiState.mergeToppings(incoming: List<EditableTopping>): CanvasToppingArrangeUiState {
    val incomingIds = incoming.mapTo(mutableSetOf()) { it.parfaitImageId }
    val localById = toppings.associateBy { it.parfaitImageId }

    val removedIds = pendingDeleteToppingIds + deletedToppingIds

    val merged = incoming
        .filterNot { it.parfaitImageId in removedIds }
        .map { stored ->
            if (stored.parfaitImageId in dirtyToppingIds) localById[stored.parfaitImageId] ?: stored else stored
        }
    val focusedId = (focusedKey as? CanvasToppingKey.Image)?.id
    val loseFocus = focusedId != null && (focusedId !in incomingIds || focusedId in removedIds)

    return copy(
        toppings = merged,
        // 목록에서 사라진 것은 세 집합에서도 뺀다 — 없는 토핑을 갱신·삭제할 수 없고,
        // 툼스톤도 제 역할을 다했다
        dirtyToppingIds = dirtyToppingIds intersect incomingIds,
        pendingDeleteToppingIds = pendingDeleteToppingIds intersect incomingIds,
        deletedToppingIds = deletedToppingIds intersect incomingIds,
        focusedKey = if (loseFocus) null else focusedKey,
        // 패널은 포커스된 토핑의 것이라 포커스와 함께 닫힌다
        isBorderPanelOpen = isBorderPanelOpen && !loseFocus,
    )
}

/** [mergeToppings] 와 같은 규칙을 텍스트 쪽 집합에 쓴다 */
internal fun CanvasToppingArrangeUiState.mergeTexts(incoming: List<EditableText>): CanvasToppingArrangeUiState {
    val incomingIds = incoming.mapTo(mutableSetOf()) { it.parfaitTextId }
    val localById = texts.associateBy { it.parfaitTextId }

    val removedIds = pendingDeleteTextIds + deletedTextIds

    val merged = incoming
        .filterNot { it.parfaitTextId in removedIds }
        .map { stored ->
            if (stored.parfaitTextId in dirtyTextIds) localById[stored.parfaitTextId] ?: stored else stored
        }
    val focusedId = (focusedKey as? CanvasToppingKey.Text)?.id
    val loseFocus = focusedId != null && (focusedId !in incomingIds || focusedId in removedIds)

    return copy(
        texts = merged,
        dirtyTextIds = dirtyTextIds intersect incomingIds,
        pendingDeleteTextIds = pendingDeleteTextIds intersect incomingIds,
        deletedTextIds = deletedTextIds intersect incomingIds,
        focusedKey = if (loseFocus) null else focusedKey,
    )
}

/** 포커스를 옮기면 이전 대상의 가이드 원값이 새 대상에 이어지지 않게 비운다 */
internal fun CanvasToppingArrangeUiState.withFocus(key: CanvasToppingKey): CanvasToppingArrangeUiState = copy(
    focusedKey = key,
    rawFocusedPositionX = null,
    rawFocusedPositionY = null,
    isCenterGuideXVisible = false,
    isCenterGuideYVisible = false,
    rawFocusedRotationDegrees = null,
    hasFocusedRotated = false,
    isAngleGuideVisible = false,
)

internal fun CanvasToppingArrangeUiState.withEmptyCanvasClicked(): CanvasToppingArrangeUiState =
    if (isBorderPanelOpen) copy(isBorderPanelOpen = false) else copy(focusedKey = null)

/** 패널이 열려 있으면 삭제 버튼 탭은 패널을 닫는 데만 쓴다 */
internal fun CanvasToppingArrangeUiState.withDeleteFocusedClicked(): CanvasToppingArrangeUiState {
    val focused = focusedKey

    return when {
        isBorderPanelOpen -> copy(isBorderPanelOpen = false)

        focused == null -> this

        focused is CanvasToppingKey.Image -> copy(
            toppings = toppings.filterNot { it.parfaitImageId == focused.id },
            pendingDeleteToppingIds = pendingDeleteToppingIds + focused.id,
            dirtyToppingIds = dirtyToppingIds - focused.id,
            focusedKey = null,
        )

        focused is CanvasToppingKey.Text -> markTextDeleted(focused.id)

        else -> this
    }
}

private fun CanvasToppingArrangeUiState.markTextDeleted(textId: Long): CanvasToppingArrangeUiState = copy(
    texts = texts.filterNot { it.parfaitTextId == textId },
    pendingDeleteTextIds = pendingDeleteTextIds + textId,
    dirtyTextIds = dirtyTextIds - textId,
    focusedKey = if (focusedKey == CanvasToppingKey.Text(textId)) null else focusedKey,
)

internal fun CanvasToppingArrangeUiState.withTransformGestureEnded(): CanvasToppingArrangeUiState = copy(
    isCenterGuideXVisible = false,
    isCenterGuideYVisible = false,
    isAngleGuideVisible = false,
    hasFocusedRotated = false,
)

internal fun CanvasToppingArrangeUiState.withBorderColorSelected(colorArgb: Int?): CanvasToppingArrangeUiState {
    val focusedId = focusedTopping?.parfaitImageId ?: return this
    val border = panelBorderForColor(colorArgb, panelBorderWidthDp)

    return copy(
        toppings = toppings.map { topping ->
            if (topping.parfaitImageId == focusedId) topping.copy(border = border) else topping
        },
    ).markDirty(focusedId)
}

/**
 * 테두리가 없는 토핑은 굵기를 [CanvasToppingArrangeUiState.pendingBorderWidthDp] 에만 담는다 —
 * 바뀐 값이 없는 토핑을 dirty 로 만들면 확인이 같은 "테두리 없음"을 저장하러 나간다.
 */
internal fun CanvasToppingArrangeUiState.withBorderWidthChanged(widthDp: Float): CanvasToppingArrangeUiState {
    val focusedId = focusedTopping?.parfaitImageId ?: return this
    val clampedWidthDp = clampPanelBorderWidthDp(widthDp)

    if (focusedTopping?.border == null) return copy(pendingBorderWidthDp = clampedWidthDp)

    return copy(
        pendingBorderWidthDp = clampedWidthDp,
        toppings = toppings.map { topping ->
            if (topping.parfaitImageId == focusedId) {
                topping.copy(border = topping.border.withPanelWidth(clampedWidthDp))
            } else {
                topping
            }
        },
    ).markDirty(focusedId)
}

/**
 * 포커스된 토핑/텍스트의 이번 프레임 중앙·각도 가이드 판정. `rawFocusedPositionX/Y` 가 아직
 * 없으면(막 포커스가 바뀐 첫 프레임) 지금 보이는 위치에서 시작한다.
 *
 * @return 포커스가 없거나 포커스된 대상이 목록에 없으면 null.
 */
internal fun CanvasToppingArrangeUiState.focusedCenterGuideStep(
    intent: CanvasToppingArrangeIntent.OnToppingTransform,
): CenterGuideStep? = when (val focused = focusedKey) {
    null -> null

    is CanvasToppingKey.Image -> toppings.firstOrNull { it.parfaitImageId == focused.id }?.let { topping ->
        stepCenterGuide(topping.positionX, topping.positionY, topping.rotationDegrees, intent)
    }

    is CanvasToppingKey.Text -> texts.firstOrNull { it.parfaitTextId == focused.id }?.let { text ->
        stepCenterGuide(text.positionX, text.positionY, text.rotationDegrees, intent)
    }
}

/** 캔버스 밖으로 나가는 부분은 화면에서 클립되어 안 보이므로 여기서 위치를 되돌리지 않는다 */
internal fun CanvasToppingArrangeUiState.withFocusedTransform(
    step: CenterGuideStep,
    zoom: Float,
): CanvasToppingArrangeUiState = when (val focused = focusedKey) {
    null -> this

    is CanvasToppingKey.Image -> copy(
        toppings = toppings.map { topping ->
            if (topping.parfaitImageId != focused.id) {
                topping
            } else {
                topping.copy(
                    positionX = step.guideX.effectiveValue,
                    positionY = step.guideY.effectiveValue,
                    scale = (topping.scale * zoom).coerceAtLeast(TOPPING_MIN_SCALE),
                    rotationDegrees = step.angle.effectiveDegrees,
                )
            }
        },
    ).markDirty(focused.id).withCenterGuideStep(step)

    is CanvasToppingKey.Text -> copy(
        texts = texts.map { text ->
            if (text.parfaitTextId != focused.id) {
                text
            } else {
                text.copy(
                    positionX = step.guideX.effectiveValue,
                    positionY = step.guideY.effectiveValue,
                    scale = (text.scale * zoom).coerceAtLeast(TEXT_TOPPING_MIN_SCALE),
                    rotationDegrees = step.angle.effectiveDegrees,
                )
            }
        },
        dirtyTextIds = dirtyTextIds + focused.id,
    ).withCenterGuideStep(step)
}

private fun CanvasToppingArrangeUiState.stepCenterGuide(
    currentPositionX: Float,
    currentPositionY: Float,
    currentRotationDegrees: Float,
    intent: CanvasToppingArrangeIntent.OnToppingTransform,
): CenterGuideStep {
    val oldRawX = rawFocusedPositionX ?: currentPositionX
    val oldRawY = rawFocusedPositionY ?: currentPositionY
    val oldRawRotation = rawFocusedRotationDegrees ?: currentRotationDegrees

    val wasSnappedX = resolveCenterGuideAxisRatio(oldRawX, intent.thresholdRatioX).isSnapped
    val wasSnappedY = resolveCenterGuideAxisRatio(oldRawY, intent.thresholdRatioY).isSnapped

    val rawX = oldRawX + intent.panX
    val rawY = oldRawY + intent.panY
    val guideX = resolveCenterGuideAxisRatio(rawX, intent.thresholdRatioX)
    val guideY = resolveCenterGuideAxisRatio(rawY, intent.thresholdRatioY)
    val angle = stepAngleGuide(
        oldRawDegrees = oldRawRotation,
        oldHasRotated = hasFocusedRotated,
        rotationDelta = intent.rotationDelta,
    )
    val justSnapped = (guideX.isSnapped && !wasSnappedX) ||
        (guideY.isSnapped && !wasSnappedY) ||
        angle.justSnapped
    return CenterGuideStep(rawX, rawY, guideX, guideY, angle, justSnapped)
}

private fun CanvasToppingArrangeUiState.withCenterGuideStep(step: CenterGuideStep): CanvasToppingArrangeUiState = copy(
    rawFocusedPositionX = step.rawX,
    rawFocusedPositionY = step.rawY,
    isCenterGuideXVisible = step.guideX.isSnapped,
    isCenterGuideYVisible = step.guideY.isSnapped,
    rawFocusedRotationDegrees = step.angle.rawDegrees,
    hasFocusedRotated = step.angle.hasRotated,
    isAngleGuideVisible = step.angle.isVisible,
    isAngleGuideHorizontal = step.angle.isHorizontal,
)

private fun CanvasToppingArrangeUiState.markDirty(toppingId: Long): CanvasToppingArrangeUiState =
    copy(dirtyToppingIds = dirtyToppingIds + toppingId)

/**
 * 저장 대상은 지금 목록에 있으면서 손댄 토핑뿐이다. 대조를 dirty 안에서만 하는 것이 요점이다:
 * 목록 전체를 스냅샷과 견주면 나중에 들어온 새 토핑이 "스냅샷에 없음 = 바뀜"으로 잡힌다.
 *
 * @param storedToppings 마지막으로 받은 그대로의 토핑. 손댄 토핑의 어느 축이 바뀌었는지 가린다
 */
internal fun CanvasToppingArrangeUiState.dirtyChanges(
    storedToppings: List<EditableTopping>,
    storedTexts: List<EditableText>,
): ToppingArrangeChanges {
    val dirty = toppings.filter { it.parfaitImageId in dirtyToppingIds }
    val dirtyTexts = texts.filter { it.parfaitTextId in dirtyTextIds }

    return ToppingArrangeChanges(
        transformChanged = dirty.filter { it.hasTransformChange(storedToppings) },
        textUpdates = dirtyTexts.mapNotNull { it.toTransformUpdateOrNull(storedTexts) },
        borderChanged = dirty.filter { it.hasBorderChange(storedToppings) },
    )
}

/** 스냅샷에 없는 토핑을 바뀐 것으로 봐도 안전하다 — 나중에 들어온 토핑은 dirty 에 안 들어온다 */
private fun EditableTopping.hasTransformChange(storedToppings: List<EditableTopping>): Boolean {
    val original = storedToppings.find { it.parfaitImageId == parfaitImageId } ?: return true
    return positionX != original.positionX ||
        positionY != original.positionY ||
        scale != original.scale ||
        rotationDegrees != original.rotationDegrees
}

private fun EditableTopping.hasBorderChange(storedToppings: List<EditableTopping>): Boolean {
    val original = storedToppings.find { it.parfaitImageId == parfaitImageId } ?: return true
    return border != original.border
}

/** 겹침 순서는 안 보낸다 — 앱에 z 조작 경로가 없어 저장된 값을 그대로 둔다 */
internal fun EditableTopping.toTransformUpdate(): ToppingTransformUpdate = ToppingTransformUpdate(
    parfaitImageId = ParfaitImageId(parfaitImageId),
    positionX = positionX.toDouble(),
    positionY = positionY.toDouble(),
    scale = scale.toDouble(),
    rotation = rotationDegrees.toDouble(),
)

/**
 * 바뀐 축만 싣는다 — null 인 축은 저장소가 유지한다. 스냅샷에 없으면 네 축을 모두 싣는다.
 *
 * @return 바뀐 축이 없으면 null.
 */
private fun EditableText.toTransformUpdateOrNull(storedTexts: List<EditableText>): TextToppingTransformUpdate? {
    val original = storedTexts.find { it.parfaitTextId == parfaitTextId }

    val update = TextToppingTransformUpdate(
        parfaitTextId = ParfaitTextId(parfaitTextId),
        positionX = changedAxisOrNull(positionX, original?.positionX),
        positionY = changedAxisOrNull(positionY, original?.positionY),
        scale = changedAxisOrNull(scale, original?.scale),
        rotation = changedAxisOrNull(rotationDegrees, original?.rotationDegrees),
    )
    val hasChange = listOf(update.positionX, update.positionY, update.scale, update.rotation).any { it != null }
    return update.takeIf { hasChange }
}

private fun changedAxisOrNull(
    value: Float,
    originalValue: Float?,
): Double? = value.takeIf { originalValue == null || abs(it - originalValue) > TEXT_TRANSFORM_TOLERANCE }?.toDouble()
