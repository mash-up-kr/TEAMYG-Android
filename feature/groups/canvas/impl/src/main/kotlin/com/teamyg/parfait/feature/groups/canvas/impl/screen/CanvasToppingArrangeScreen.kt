package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarTitle
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.outline.rememberToppingOutlines
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.component.AngleGuideLines
import com.teamyg.parfait.feature.groups.canvas.impl.component.CenterGuideLines
import com.teamyg.parfait.feature.groups.canvas.impl.component.EditableLayerEntry
import com.teamyg.parfait.feature.groups.canvas.impl.component.EditableTextTopping
import com.teamyg.parfait.feature.groups.canvas.impl.component.EditableToppingImage
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingArrangeCanvasDim
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingArrangeCanvasSurface
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingArrangeLayout
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingBorderPanel
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingFocusDecoration
import com.teamyg.parfait.feature.groups.canvas.impl.component.editableLayerOrder
import com.teamyg.parfait.feature.groups.canvas.impl.component.rememberEditableTextEntries
import com.teamyg.parfait.feature.groups.canvas.impl.component.rememberEditableToppingDrawEntries
import com.teamyg.parfait.feature.groups.canvas.impl.component.rememberEditableToppingHitEntries
import com.teamyg.parfait.feature.groups.canvas.impl.component.toppingPanelInputs
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasToppingKey
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableText
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingType
import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingBorderStyle
import com.teamyg.parfait.feature.groups.canvas.impl.util.CENTER_GUIDE_THRESHOLD_DP
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BORDER_WIDTH_RANGE_DP
import com.teamyg.parfait.feature.groups.canvas.impl.util.animatePanelFocusFraction
import com.teamyg.parfait.feature.groups.canvas.impl.util.animateToppingSnapSettleDp
import com.teamyg.parfait.feature.groups.canvas.impl.util.animateToppingSnapSettleRotation
import com.teamyg.parfait.feature.groups.canvas.impl.util.panelFocusCenter
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingArrangeUiState

private const val INPUT_TAG = "topping_arrange_input"

/**
 * 이미 캔버스에 놓인 본인 토핑을 골라 옮기고 테두리를 정하는 화면.
 *
 * 테두리 패널이 열린 동안에는 포커스된 토핑이 패널에 가리지 않는 자리로 옮겨 보이고 다른 본인
 * 토핑 위에 그려진다 — 보이는 자리와 그리는 순서만 바뀌고 [CanvasToppingArrangeUiState] 의
 * 위치와 목록 순서는 그대로다.
 *
 * @param onToppingTransform `panX`/`panY` 는 px 가 아니라 Canvas-Area 대비 비율이다.
 * @param onToppingTransformGestureEnd 이동 제스처가 끝났다(손을 뗐다). 중앙 가이드를 끈다.
 * @param toastPolicy 이 화면이 헤더 아래 캔버스 윗변에 직접 띄운다. 로딩 덮개보다 아래 층이다.
 */
@Composable
internal fun CanvasToppingArrangeScreen(
    uiState: CanvasToppingArrangeUiState,
    onClickClose: () -> Unit,
    onClickConfirm: () -> Unit,
    onClickTopping: (EditableTopping) -> Unit,
    onClickText: (EditableText) -> Unit,
    onClickEmptyCanvas: () -> Unit,
    onToggleBorderPanel: () -> Unit,
    onDismissBorderPanel: () -> Unit,
    onSelectBorderColor: (Int?) -> Unit,
    onChangeBorderWidth: (Float) -> Unit,
    onToppingTransform: (
        panX: Float,
        panY: Float,
        zoom: Float,
        rotationDelta: Float,
        thresholdRatioX: Float,
        thresholdRatioY: Float,
    ) -> Unit,
    onToppingTransformGestureEnd: () -> Unit,
    onClickDeleteTopping: () -> Unit,
    modifier: Modifier = Modifier,
    toastPolicy: YGToastPolicy = rememberYGToastPolicy(),
) {
    ToppingArrangeLayout(
        toast = { YGToastHost(policy = toastPolicy, modifier = Modifier.fillMaxWidth()) },
        header = {
            YGFloatingBarTitle(
                title = stringResource(R.string.canvas_topping_place_title),
                onCloseClick = onClickClose,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        onClickConfirm = onClickConfirm,
        panel = {
            if (uiState.canOpenBorderPanel) {
                ToppingBorderPanel(
                    isOpen = uiState.isBorderPanelOpen,
                    selectedColorArgb = uiState.panelBorderColorArgb,
                    widthDp = uiState.panelBorderWidthDp,
                    widthRange = TOPPING_BORDER_WIDTH_RANGE_DP,
                    onClickToggle = onToggleBorderPanel,
                    onSelectColor = onSelectBorderColor,
                    onChangeWidth = onChangeBorderWidth,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
        },
        modifier = modifier,
    ) {
        // 배치가 모두 이 영역 대비 비율이라, 캔버스 메인과 같은 자리에 그리려면 실제 크기를 알아야 한다
        BoxWithConstraints(modifier = Modifier.matchParentSize()) {
            val canvasWidth = maxWidth
            val canvasHeight = maxHeight
            val density = LocalDensity.current
            val canvasWidthPx = with(density) { canvasWidth.toPx() }
            val canvasHeightPx = with(density) { canvasHeight.toPx() }
            // ViewModel 은 캔버스의 실제 dp/px 크기를 모르므로, 중앙 가이드 임계값(dp)을
            // 이 프레임의 캔버스 크기로 나눠 비율로 미리 바꿔 보낸다
            val centerGuideThresholdPx = with(density) { CENTER_GUIDE_THRESHOLD_DP.dp.toPx() }
            val thresholdRatioX = centerGuideThresholdPx / canvasWidthPx
            val thresholdRatioY = centerGuideThresholdPx / canvasHeightPx

            val drawEntries = rememberEditableToppingDrawEntries(
                toppings = uiState.toppings,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
            )
            val outlines = rememberToppingOutlines(
                models = drawEntries.map { it.topping.imageUrl },
                retryKey = 0,
            )

            // 저장된 자리로 만든다. 터치 판정은 패널이 열린 동안의 임시 자리가 아니라 이것으로 한다
            val imageEntries = rememberEditableToppingHitEntries(drawEntries, outlines)
            // scale 을 곱하기 전 크기다
            val textBoxSizes = remember { mutableStateMapOf<Long, IntSize>() }
            val textEntries = rememberEditableTextEntries(
                texts = uiState.texts,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                boxSizes = textBoxSizes,
            )
            val layers = editableLayerOrder(images = imageEntries, texts = textEntries)
            val othersLayers = layers.filterNot { it.isMine }
            val myLayers = layers.filter { it.isMine }
            val focusedLayer = myLayers.firstOrNull { it.key == uiState.focusedKey }
            var isToppingGestureActive by remember { mutableStateOf(false) }

            var wasToppingGestureActive by remember { mutableStateOf(false) }
            LaunchedEffect(isToppingGestureActive) {
                if (wasToppingGestureActive && !isToppingGestureActive) onToppingTransformGestureEnd()
                wasToppingGestureActive = isToppingGestureActive
            }

            // 그리는 자리. 이미지와 선택 박스가 같은 자리에 오려면 같은 값을 봐야 한다.
            // 패널은 사진에만 열리므로 텍스트는 늘 제자리다
            val focusFraction by animatePanelFocusFraction(uiState.isBorderPanelOpen)

            val focusedRawCenter = when (focusedLayer) {
                is EditableLayerEntry.Image -> focusedLayer.entry.draw.center
                is EditableLayerEntry.Text -> focusedLayer.entry.center
                null -> null
            }
            val focusedRawRotationDegrees = when (focusedLayer) {
                is EditableLayerEntry.Image -> focusedLayer.entry.topping.rotationDegrees
                is EditableLayerEntry.Text -> focusedLayer.entry.text.rotationDegrees
                null -> null
            }

            val animatedFocusedCenterX by
                animateToppingSnapSettleDp(focusedRawCenter?.x ?: 0.dp, uiState.isCenterGuideXVisible)
            val animatedFocusedCenterY by
                animateToppingSnapSettleDp(focusedRawCenter?.y ?: 0.dp, uiState.isCenterGuideYVisible)
            val animatedFocusedRotationDegrees by
                animateToppingSnapSettleRotation(focusedRawRotationDegrees ?: 0f, uiState.isAngleGuideVisible)
            val animatedFocusedRawCenter = focusedRawCenter
                ?.let { DpOffset(animatedFocusedCenterX, animatedFocusedCenterY) }

            val focusedDrawnCenter = when (focusedLayer) {
                is EditableLayerEntry.Image ->
                    animatedFocusedRawCenter?.let { rawCenter ->
                        lerp(
                            rawCenter,
                            panelFocusCenter(DpSize(canvasWidth, canvasHeight)),
                            focusFraction,
                        )
                    }

                is EditableLayerEntry.Text -> animatedFocusedRawCenter

                null -> null
            }

            @Composable
            fun Layer(
                layer: EditableLayerEntry,
                onClick: (() -> Unit)?,
            ) {
                when (layer) {
                    is EditableLayerEntry.Image -> EditableToppingImage(
                        entry = layer.entry.draw,
                        outline = outlines[layer.entry.topping.imageUrl],
                        alpha = 1f,
                        onClick = onClick,
                        centerOverride = focusedDrawnCenter.takeIf { layer === focusedLayer },
                        rotationOverride = animatedFocusedRotationDegrees.takeIf { layer === focusedLayer },
                    )

                    is EditableLayerEntry.Text -> EditableTextTopping(
                        entry = layer.entry,
                        canvasWidth = canvasWidth,
                        onBoxSizeChange = { size -> textBoxSizes[layer.entry.text.parfaitTextId] = size },
                        onClick = onClick,
                        centerOverride = focusedDrawnCenter.takeIf { layer === focusedLayer },
                        rotationOverride = animatedFocusedRotationDegrees.takeIf { layer === focusedLayer },
                    )
                }
            }

            fun onHit(layer: EditableLayerEntry) = when (layer) {
                is EditableLayerEntry.Image -> onClickTopping(layer.entry.topping)
                is EditableLayerEntry.Text -> onClickText(layer.entry.text)
            }

            ToppingArrangeCanvasSurface(
                backgroundColor = uiState.backgroundColor,
                backgroundImageUrl = uiState.backgroundImageUrl,
                modifier = Modifier.matchParentSize(),
            ) {
                othersLayers.forEach { layer ->
                    key(layer.key) { Layer(layer = layer, onClick = null) }
                }

                ToppingArrangeCanvasDim()

                // 가이드라인이 "포커스 안 된 본인 토핑"보다 위, "이동 중 토핑"보다 아래에
                // 오도록 포커스된 레이어를 따로 떼 맨 마지막에 그린다(스펙 레이어 순서)
                myLayers.filterNot { it === focusedLayer }.forEach { layer ->
                    key(layer.key) { Layer(layer = layer, onClick = { onHit(layer) }) }
                }

                CenterGuideLines(
                    showVertical = uiState.isCenterGuideXVisible,
                    showHorizontal = uiState.isCenterGuideYVisible,
                    modifier = Modifier.matchParentSize(),
                )

                // 위치 가이드 위, 이동 중 토핑 아래(기획 레이어 순서)
                AngleGuideLines(
                    isVisible = uiState.isAngleGuideVisible,
                    isHorizontal = uiState.isAngleGuideHorizontal,
                    centerOffset = (focusedDrawnCenter ?: DpOffset.Zero)
                        .let { if (uiState.isAngleGuideHorizontal) it.y else it.x },
                    modifier = Modifier.matchParentSize(),
                )

                focusedLayer?.let { layer ->
                    key(layer.key) { Layer(layer = layer, onClick = { onHit(layer) }) }
                }

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .testTag(INPUT_TAG)
                        .toppingPanelInputs(
                            isPanelOpen = { uiState.isBorderPanelOpen },
                            onDismissPanel = onDismissBorderPanel,
                            // 아래에서 위 순서 — 본인 토핑이 남의 토핑 위에 그려진다
                            tapEntries = {
                                (othersLayers + myLayers).mapNotNull { layer -> layer.target?.let { layer to it } }
                            },
                            tapKeyOf = { it.key },
                            onTapHit = ::onHit,
                            onTapMiss = onClickEmptyCanvas,
                            transformTargetAt = { focusedLayer?.target },
                            onTransform = { pan, zoom, rotationDelta ->
                                onToppingTransform(
                                    pan.x / canvasWidthPx,
                                    pan.y / canvasHeightPx,
                                    zoom,
                                    rotationDelta,
                                    thresholdRatioX,
                                    thresholdRatioY,
                                )
                            },
                            onGestureActiveChange = { isToppingGestureActive = it },
                        ),
                )
            }

            // 선택 박스와 삭제 버튼은 토핑 이미지와 달리 캔버스를 넘어가도 잘리면 안 된다
            val focusedTarget = focusedLayer?.target
            if (focusedTarget != null && focusedDrawnCenter != null) {
                ToppingFocusDecoration(
                    target = focusedTarget,
                    center = focusedDrawnCenter,
                    onClickDelete = onClickDeleteTopping,
                    showActionButtons = !isToppingGestureActive,
                    rotationOverride = animatedFocusedRotationDegrees,
                )
            }
        }
    }
}

/** 실제 리소스라야 painter 가 Success 에 닿아 테두리도 함께 그려진다 */
private const val PREVIEW_TOPPING_MODEL =
    "android.resource://com.teamyg.parfait.feature.groups.canvas.impl/drawable/nukkiii"

private class CanvasToppingArrangePreviewParameterProvider : NamedPreviewParameterProvider<Boolean>() {
    override val entries = listOf(
        "borderPanelClosed" to false,
        "borderPanelOpen" to true,
    )
}

@YGPreview
@Composable
private fun PreviewCanvasToppingArrangeScreen(
    @PreviewParameter(CanvasToppingArrangePreviewParameterProvider::class) isBorderPanelOpen: Boolean,
) = PreviewBox {
    CanvasToppingArrangeScreen(
        uiState = CanvasToppingArrangeUiState(
            toppings = listOf(
                EditableTopping(
                    parfaitImageId = 1L,
                    isMine = true,
                    imageUrl = PREVIEW_TOPPING_MODEL,
                    positionX = 0.3f,
                    positionY = 0.3f,
                    border = ToppingBorderStyle(colorArgb = 0xFFFFFFFF.toInt(), widthDp = 4f),
                ),
                EditableTopping(
                    parfaitImageId = 2L,
                    isMine = false,
                    imageUrl = PREVIEW_TOPPING_MODEL,
                    positionX = 0.7f,
                    positionY = 0.6f,
                    rotationDegrees = 15f,
                ),
            ),
            texts = listOf(
                EditableText(
                    parfaitTextId = 1L,
                    isMine = true,
                    text = "오늘의 파르페",
                    color = TextToppingColor.Black,
                    type = TextToppingType.FILL,
                    align = TextAlign.Center,
                    designFontSize = 18f,
                    positionX = 0.5f,
                    positionY = 0.8f,
                    positionZ = 3,
                ),
            ),
            focusedKey = CanvasToppingKey.Image(1L),
            isBorderPanelOpen = isBorderPanelOpen,
        ),
        onClickClose = {},
        onClickConfirm = {},
        onClickTopping = {},
        onClickText = {},
        onClickEmptyCanvas = {},
        onToggleBorderPanel = {},
        onDismissBorderPanel = {},
        onSelectBorderColor = {},
        onChangeBorderWidth = {},
        onToppingTransform = { _, _, _, _, _, _ -> },
        onToppingTransformGestureEnd = {},
        onClickDeleteTopping = {},
        modifier = Modifier.fillMaxSize(),
    )
}
