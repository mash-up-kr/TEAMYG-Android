package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.lerp
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarTitle
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.outline.rememberToppingOutlines
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.component.EditableToppingImage
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingArrangeCanvasDim
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingArrangeCanvasSurface
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingArrangeLayout
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingBorderPanel
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingFocusDecoration
import com.teamyg.parfait.feature.groups.canvas.impl.component.rememberEditableToppingDrawEntries
import com.teamyg.parfait.feature.groups.canvas.impl.component.rememberEditableToppingHitEntries
import com.teamyg.parfait.feature.groups.canvas.impl.component.toppingPanelInputs
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingBorderStyle
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BORDER_WIDTH_RANGE_DP
import com.teamyg.parfait.feature.groups.canvas.impl.util.animatePanelFocusFraction
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
 * @param toastPolicy 이 화면이 헤더 아래 캔버스 윗변에 직접 띄운다. 로딩 덮개보다 아래 층이다.
 */
@Composable
internal fun CanvasToppingArrangeScreen(
    uiState: CanvasToppingArrangeUiState,
    onClickClose: () -> Unit,
    onClickConfirm: () -> Unit,
    onClickTopping: (EditableTopping) -> Unit,
    onClickEmptyCanvas: () -> Unit,
    onToggleBorderPanel: () -> Unit,
    onDismissBorderPanel: () -> Unit,
    onSelectBorderColor: (Int?) -> Unit,
    onChangeBorderWidth: (Float) -> Unit,
    onToppingTransform: (panX: Float, panY: Float, zoom: Float, rotationDelta: Float) -> Unit,
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
            ToppingBorderPanel(
                isOpen = uiState.isBorderPanelOpen,
                selectedColorArgb = uiState.panelBorderColorArgb,
                widthDp = uiState.panelBorderWidthDp,
                widthRange = TOPPING_BORDER_WIDTH_RANGE_DP,
                onClickToggle = onToggleBorderPanel,
                onSelectColor = onSelectBorderColor,
                onChangeWidth = onChangeBorderWidth,
                isEnabled = uiState.canOpenBorderPanel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
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
            val entries = rememberEditableToppingHitEntries(drawEntries, outlines)
            val othersEntries = entries.filterNot { it.topping.isMine }
            val myEntries = entries.filter { it.topping.isMine }
            val focusedEntry = myEntries.firstOrNull { it.topping.parfaitImageId == uiState.focusedToppingId }
            var isToppingGestureActive by remember { mutableStateOf(false) }

            // 그리는 자리. 이미지와 선택 박스가 같은 자리에 오려면 같은 값을 봐야 한다
            val focusFraction by animatePanelFocusFraction(uiState.isBorderPanelOpen)
            val focusedDrawnCenter = focusedEntry?.let { entry ->
                lerp(entry.draw.center, panelFocusCenter(DpSize(canvasWidth, canvasHeight)), focusFraction)
            }

            // zIndex 가 아니라 컴포지션 순서로 올린다 — 겹침 순서가 노드 순서와 갈리지 않는다.
            // 패널 상태가 아니라 애니메이션을 본다 — 제자리로 돌아오는 동안에도 위에 있어야 한다
            val myEntriesInDrawOrder = if (focusFraction > 0f && focusedEntry != null) {
                myEntries.filterNot { it === focusedEntry } + focusedEntry
            } else {
                myEntries
            }

            ToppingArrangeCanvasSurface(
                backgroundColor = uiState.backgroundColor,
                backgroundImageUrl = uiState.backgroundImageUrl,
                modifier = Modifier.matchParentSize(),
            ) {
                othersEntries.forEach { entry ->
                    key(entry.topping.parfaitImageId) {
                        EditableToppingImage(
                            entry = entry.draw,
                            outline = outlines[entry.topping.imageUrl],
                            alpha = 1f,
                            onClick = null,
                        )
                    }
                }

                ToppingArrangeCanvasDim()

                myEntriesInDrawOrder.forEach { entry ->
                    key(entry.topping.parfaitImageId) {
                        EditableToppingImage(
                            entry = entry.draw,
                            outline = outlines[entry.topping.imageUrl],
                            alpha = 1f,
                            onClick = { onClickTopping(entry.topping) },
                            centerOverride = focusedDrawnCenter.takeIf { entry === focusedEntry },
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .testTag(INPUT_TAG)
                        .toppingPanelInputs(
                            isPanelOpen = { uiState.isBorderPanelOpen },
                            onDismissPanel = onDismissBorderPanel,
                            // 아래에서 위 순서 — 본인 토핑이 남의 토핑 위에 그려진다
                            tapEntries = { (othersEntries + myEntries).map { it.topping to it.target } },
                            tapKeyOf = { it.parfaitImageId },
                            onTapHit = onClickTopping,
                            onTapMiss = onClickEmptyCanvas,
                            transformTargetAt = { focusedEntry?.target },
                            onTransform = { pan, zoom, rotationDelta ->
                                onToppingTransform(
                                    pan.x / canvasWidthPx,
                                    pan.y / canvasHeightPx,
                                    zoom,
                                    rotationDelta,
                                )
                            },
                            onGestureActiveChange = { isToppingGestureActive = it },
                        ),
                )
            }

            // 선택 박스와 삭제 버튼은 토핑 이미지와 달리 캔버스를 넘어가도 잘리면 안 된다
            if (focusedEntry != null && focusedDrawnCenter != null) {
                ToppingFocusDecoration(
                    entry = focusedEntry,
                    center = focusedDrawnCenter,
                    onClickDelete = onClickDeleteTopping,
                    showActionButtons = !isToppingGestureActive,
                )
            }
        }
    }
}

/** 실제 리소스라야 painter 가 Success 에 닿아 테두리도 함께 그려진다 */
private const val PREVIEW_TOPPING_MODEL =
    "android.resource://com.teamyg.parfait.feature.groups.canvas.impl/drawable/nukkiii"

private class CanvasToppingArrangePreviewParameterProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(false, true)
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
            focusedToppingId = 1L,
            isBorderPanelOpen = isBorderPanelOpen,
        ),
        onClickClose = {},
        onClickConfirm = {},
        onClickTopping = {},
        onClickEmptyCanvas = {},
        onToggleBorderPanel = {},
        onDismissBorderPanel = {},
        onSelectBorderColor = {},
        onChangeBorderWidth = {},
        onToppingTransform = { _, _, _, _ -> },
        onClickDeleteTopping = {},
        modifier = Modifier.fillMaxSize(),
    )
}
