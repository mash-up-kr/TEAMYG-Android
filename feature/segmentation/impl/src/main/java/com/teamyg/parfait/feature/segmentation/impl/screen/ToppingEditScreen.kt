package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygeditactionbutton.YGEditActionButton
import com.teamyg.parfait.core.designsystem.component.ygeditbutton.YGEditButton
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarEditTab
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.R
import com.teamyg.parfait.feature.segmentation.impl.component.BorderColorChipRow
import com.teamyg.parfait.feature.segmentation.impl.component.BrushWidthSlider
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditMode
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditStroke
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditTab
import com.teamyg.parfait.feature.segmentation.impl.editor.UndoRedoStack
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.ToppingEditState
import kotlin.math.roundToInt
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/** 두 손가락으로 넓힐 수 있는 배율. 1배는 화면에 꽉 맞춘 처음 배치라 그보다 작게는 줄이지 않는다 */
private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 3f

/** 지워진 자리에 남겨 둘 원본의 불투명도 */
private const val ERASED_AREA_ALPHA = 0.5f

private const val BRUSH_PREVIEW_FILL_ALPHA = 0.5f
private val BRUSH_PREVIEW_BORDER_WIDTH = 1.dp

@Composable
internal fun ToppingEditScreen(
    state: ToppingEditState,
    onChangeTab: (ToppingEditTab) -> Unit,
    onChangeMode: (ToppingEditMode) -> Unit,
    onChangeBrushWidth: (Float) -> Unit,
    onAddStroke: (ToppingEditStroke) -> Unit,
    onClickUndoArea: () -> Unit,
    onClickRedoArea: () -> Unit,
    onSelectBorderColor: (Color) -> Unit,
    onChangeBorderWidth: (Float) -> Unit,
    onClickUndoBorder: () -> Unit,
    onClickRedoBorder: () -> Unit,
    onClickDone: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        ToppingEditContent(
            state = state,
            onChangeTab = onChangeTab,
            onChangeMode = onChangeMode,
            onChangeBrushWidth = onChangeBrushWidth,
            onAddStroke = onAddStroke,
            onClickUndoArea = onClickUndoArea,
            onClickRedoArea = onClickRedoArea,
            onSelectBorderColor = onSelectBorderColor,
            onChangeBorderWidth = onChangeBorderWidth,
            onClickUndoBorder = onClickUndoBorder,
            onClickRedoBorder = onClickRedoBorder,
            onClickDone = onClickDone,
            onClickBack = onClickBack,
            modifier = Modifier.fillMaxSize(),
        )

        if (state.isSaving) {
            ToppingEditSavingOverlay(modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
private fun ToppingEditContent(
    state: ToppingEditState,
    onChangeTab: (ToppingEditTab) -> Unit,
    onChangeMode: (ToppingEditMode) -> Unit,
    onChangeBrushWidth: (Float) -> Unit,
    onAddStroke: (ToppingEditStroke) -> Unit,
    onClickUndoArea: () -> Unit,
    onClickRedoArea: () -> Unit,
    onSelectBorderColor: (Color) -> Unit,
    onChangeBorderWidth: (Float) -> Unit,
    onClickUndoBorder: () -> Unit,
    onClickRedoBorder: () -> Unit,
    onClickDone: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 붓 크기 미리보기는 슬라이더를 잡고 있는 동안만 띄운다. 화면 안에서만 쓰는 상태다
    var isAdjustingBrushWidth by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        val historyModifier = Modifier
            .fillMaxWidth()
            .padding(YGTheme.layout.padding.padding7)

        when (state.tab) {
            ToppingEditTab.AREA -> ToppingEditHistoryActions(
                history = state.areaHistory,
                onClickUndo = onClickUndoArea,
                onClickRedo = onClickRedoArea,
                modifier = historyModifier,
            )

            ToppingEditTab.BORDER -> ToppingEditHistoryActions(
                history = state.borderHistory,
                onClickUndo = onClickUndoBorder,
                onClickRedo = onClickRedoBorder,
                modifier = historyModifier,
            )
        }

        Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap6))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = YGTheme.layout.padding.padding7)
                .weight(1f),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()

                state.tab == ToppingEditTab.BORDER -> ToppingBorderEditScreen(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> ToppingEditCanvas(
                    state = state,
                    onAddStroke = onAddStroke,
                    isBrushPreviewVisible = isAdjustingBrushWidth,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.height(23.dp))

        when (state.tab) {
            ToppingEditTab.AREA -> SegmentationAreaControls(
                state = state,
                onChangeMode = onChangeMode,
                onChangeBrushWidth = { width ->
                    isAdjustingBrushWidth = true
                    onChangeBrushWidth(width)
                },
                onChangeBrushWidthFinished = { isAdjustingBrushWidth = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = YGTheme.layout.padding.padding6,
                        start = YGTheme.layout.padding.padding7,
                        end = YGTheme.layout.padding.padding7,
                    ),
            )

            // 색상칩이 화면 끝까지 스크롤되도록 가로 여백은 컨트롤 안에서 항목별로 준다
            ToppingEditTab.BORDER -> SegmentationBorderControls(
                state = state,
                onSelectColor = onSelectBorderColor,
                onChangeWidth = onChangeBorderWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = YGTheme.layout.padding.padding6),
            )
        }

        YGFloatingBarEditTab(
            tabs = ToppingEditTab.entries.map { tab -> stringResource(tab.label) },
            selectedIndex = state.tab.ordinal,
            onTabSelect = { index -> onChangeTab(ToppingEditTab.entries[index]) },
            onCloseClick = onClickBack,
            onConfirmClick = onClickDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 저장이 끝날 때까지 화면을 덮는 딤.
 *
 * 덮은 동안은 뒤쪽 조작이 닿으면 안 되므로 눌림도 여기서 삼킨다.
 * 맨 앞에서 먼저 받는 [PointerEventPass.Initial] 단계에 삼켜야 뒤쪽 획과 버튼이 함께 막힌다.
 */
@Composable
private fun ToppingEditSavingOverlay(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(YGAtomicColors.Transparency.Black50)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { change -> change.consume() }
                    }
                }
            },
    ) {
        CircularProgressIndicator(color = YGAtomicColors.Cherry.Cherry100)
    }
}

/** 어느 탭의 스택이든 되돌릴 수 있는지만 보므로 [history] 가 무엇을 쌓는지는 알 필요가 없다 */
@Composable
private fun ToppingEditHistoryActions(
    history: UndoRedoStack<*>,
    onClickUndo: () -> Unit,
    onClickRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
        modifier = modifier,
    ) {
        YGEditActionButton(
            iconResource = DesignSystemR.drawable.ic_arrow_left,
            contentDescription = stringResource(R.string.topping_edit_undo),
            onClick = onClickUndo,
            isEnabled = history.canUndo,
        )
        YGEditActionButton(
            iconResource = DesignSystemR.drawable.ic_arrow_right,
            contentDescription = stringResource(R.string.topping_edit_redo),
            onClick = onClickRedo,
            isEnabled = history.canRedo,
        )
    }
}

@Composable
private fun ToppingEditCanvas(
    state: ToppingEditState,
    onAddStroke: (ToppingEditStroke) -> Unit,
    isBrushPreviewVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val originBitmap = state.originBitmap ?: return
    val segmentationBitmap = state.segmentationBitmap ?: return

    // 그리는 도중의 획. 매 포인터 이벤트마다 ViewModel 상태를 갱신하지 않도록 화면이 들고 있다가
    // 드래그가 끝날 때 한 번만 확정한다
    var drawingPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // 두 손가락으로 조절하는 확대 배율과 이동량. 사진이 바뀌면 처음 배치로 되돌린다
    var zoom by remember(originBitmap) { mutableFloatStateOf(MIN_ZOOM) }
    var pan by remember(originBitmap) { mutableStateOf(Offset.Zero) }

    val originImage = remember(originBitmap) { originBitmap.asImageBitmap() }
    val segmentationImage = remember(segmentationBitmap) { segmentationBitmap.asImageBitmap() }

    val brushWidthPx = with(LocalDensity.current) { state.brushWidthDp.dp.toPx() }

    fun commitStroke(mapping: BitmapViewMapping) {
        // 붓 굵기는 화면 기준이라 확대 배율을 걷어내야 원본 좌표계 굵기가 된다
        val stroke = state.strokeOrNull(drawingPoints, brushWidthPx / mapping.scale)
        drawingPoints = emptyList()
        stroke?.let(onAddStroke)
    }

    Canvas(
        modifier = modifier
            // 그리기는 레이아웃 경계를 저절로 지키지 않는다. 확대한 사진이 편집 영역을 넘어
            // 아래 조작부까지 번지지 않도록 처음 받은 자리에서 잘라낸다
            .clipToBounds()
            .pointerInput(originBitmap, state.mode, brushWidthPx) {
                val viewSize = Size(size.width.toFloat(), size.height.toFloat())
                val viewCenter = Offset(viewSize.width / 2f, viewSize.height / 2f)

                fun currentMapping(): BitmapViewMapping = BitmapViewMapping.fitCenter(
                    viewSize = viewSize,
                    bitmapWidth = originBitmap.width,
                    bitmapHeight = originBitmap.height,
                    zoom = zoom,
                    pan = pan,
                )

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // 손가락이 둘 이상이면 그리기가 아니라 확대/이동으로 넘어간다
                    var isTransforming = false
                    drawingPoints = listOf(mapViewToBitmapFloat(down.position, currentMapping()))

                    do {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }

                        if (pressedChanges.size >= 2) {
                            if (!isTransforming) {
                                // 두 번째 손가락이 닿는 순간 그리던 획은 버린다
                                isTransforming = true
                                drawingPoints = emptyList()
                            }

                            val newZoom = (zoom * event.calculateZoom()).coerceIn(MIN_ZOOM, MAX_ZOOM)
                            // 배율이 한계에 걸리면 걸린 만큼만 반영해야 이동량이 어긋나지 않는다
                            val appliedZoomChange = newZoom / zoom
                            val centroid = event.calculateCentroid(useCurrent = true) - viewCenter
                            // 두 손가락 사이 지점이 제자리에 머물도록 확대 전후의 이동량을 맞춘다
                            val zoomedPan = centroid * (1f - appliedZoomChange) + pan * appliedZoomChange

                            zoom = newZoom
                            pan = clampPan(
                                pan = zoomedPan + event.calculatePan(),
                                viewSize = viewSize,
                                bitmapWidth = originBitmap.width,
                                bitmapHeight = originBitmap.height,
                                zoom = newZoom,
                            )
                            event.changes.forEach { change -> change.consume() }
                        } else if (!isTransforming) {
                            val change = pressedChanges.firstOrNull()
                            if (change != null && change.positionChange() != Offset.Zero) {
                                drawingPoints = drawingPoints + mapViewToBitmapFloat(change.position, currentMapping())
                                change.consume()
                            }
                        }
                    } while (event.changes.any { change -> change.pressed })

                    if (isTransforming) {
                        drawingPoints = emptyList()
                    } else {
                        commitStroke(currentMapping())
                    }
                }
            },
    ) {
        val mapping = BitmapViewMapping.fitCenter(
            viewSize = size,
            bitmapWidth = originBitmap.width,
            bitmapHeight = originBitmap.height,
            zoom = zoom,
            pan = pan,
        )
        val dstOffset = IntOffset(mapping.offsetX.roundToInt(), mapping.offsetY.roundToInt())
        val dstSize = IntSize(
            width = (originBitmap.width * mapping.scale).roundToInt(),
            height = (originBitmap.height * mapping.scale).roundToInt(),
        )

        // 잘려나간 자리에 원본을 옅게 깔아 무엇을 지웠는지 보이게 한다
        drawImage(
            image = originImage,
            dstOffset = dstOffset,
            dstSize = dstSize,
            alpha = ERASED_AREA_ALPHA,
        )

        // 알파 합성을 독립된 레이어에서 해야 Clear 가 화면 전체를 뚫지 않는다
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), Paint())

            // 이미지가 놓인 자리로 잘라낸다. 원본을 덮어씌우는 SrcIn 은 이미지가 그려지는
            // 만큼만 닿기 때문에, 잘라내지 않으면 여백에 그은 검은 획이 덮이지 못하고 남는다
            clipRect(
                left = dstOffset.x.toFloat(),
                top = dstOffset.y.toFloat(),
                right = (dstOffset.x + dstSize.width).toFloat(),
                bottom = (dstOffset.y + dstSize.height).toFloat(),
            ) {
                drawImage(image = segmentationImage, dstOffset = dstOffset, dstSize = dstSize)

                // 한 리스트로 이어 붙이면 그릴 때마다 리스트가 새로 생겨 따로 그린다
                state.strokes.forEach { stroke -> drawEditStroke(stroke, mapping) }
                state
                    .strokeOrNull(drawingPoints, brushWidthPx / mapping.scale)
                    ?.let { stroke -> drawEditStroke(stroke, mapping) }

                // 마스크가 남은 자리에만 원본 픽셀을 채운다. ADD 로 칠한 곳이 원본으로 복원되는 지점
                drawImage(
                    image = originImage,
                    dstOffset = dstOffset,
                    dstSize = dstSize,
                    blendMode = BlendMode.SrcIn,
                )
            }

            canvas.restore()
        }

        // 굵기를 고르는 동안에만 실제 붓 크기를 캔버스 한가운데에 미리 보여준다
        if (isBrushPreviewVisible) {
            drawCircle(
                color = YGAtomicColors.Cherry.Cherry500.copy(alpha = BRUSH_PREVIEW_FILL_ALPHA),
                radius = brushWidthPx / 2f,
                center = center,
            )
            drawCircle(
                color = YGAtomicColors.Cherry.Cherry500,
                radius = brushWidthPx / 2f,
                center = center,
                style = Stroke(width = BRUSH_PREVIEW_BORDER_WIDTH.toPx()),
            )
        }
    }
}

private fun DrawScope.drawEditStroke(
    stroke: ToppingEditStroke,
    mapping: BitmapViewMapping,
) {
    val points = stroke.points
    val first = points.firstOrNull() ?: return
    val start = mapBitmapToViewFloat(first, mapping)

    val path = Path().apply {
        moveTo(start.x, start.y)
        if (points.size == 1) {
            // 점 하나짜리 획은 제자리에 짧은 선을 그어 둥근 점으로 찍는다
            lineTo(start.x, start.y)
        } else {
            points.drop(1).forEach { point ->
                val mapped = mapBitmapToViewFloat(point, mapping)
                lineTo(mapped.x, mapped.y)
            }
        }
    }

    drawPath(
        path = path,
        color = Color.Black,
        style = Stroke(
            width = stroke.width * mapping.scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
        // 색은 무의미하다. SrcIn 단계에서 원본으로 덮이므로 알파를 채우고 지우는 역할만 한다
        blendMode = when (stroke.mode) {
            ToppingEditMode.ADD -> BlendMode.SrcOver
            ToppingEditMode.ERASE -> BlendMode.Clear
        },
    )
}

@Composable
private fun SegmentationAreaControls(
    state: ToppingEditState,
    onChangeMode: (ToppingEditMode) -> Unit,
    onChangeBrushWidth: (Float) -> Unit,
    onChangeBrushWidthFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 라벨과 바는 한 덩어리로 붙여야 해서 바깥 Column 의 간격을 타지 않도록 따로 감싼다
        Column {
            Text(
                text = stringResource(R.string.topping_edit_brush_width),
                style = YGTheme.typography.caption.c01M,
                color = YGAtomicColors.Gray.Gray700,
            )
            Spacer(modifier = Modifier.height(4.dp))
            BrushWidthSlider(
                value = state.brushWidthDp,
                onValueChange = onChangeBrushWidth,
                onValueChangeFinished = onChangeBrushWidthFinished,
                valueRange = state.minBrushWidthDp..state.maxBrushWidthDp,
                isEnabled = !state.isLoading,
            )
        }

        // 하단 플로팅 바 바로 위에 붙어 모드 전환이 탭 전환과 한 덩어리로 보이게 둔다
        Row {
            YGEditButton(
                text = stringResource(R.string.topping_edit_area_erase),
                isSelected = state.mode == ToppingEditMode.ERASE,
                onClick = { onChangeMode(ToppingEditMode.ERASE) },
                modifier = Modifier.weight(1f),
                iconResource = DesignSystemR.drawable.ic_minus_round,
            )
            YGEditButton(
                text = stringResource(R.string.topping_edit_area_add),
                isSelected = state.mode == ToppingEditMode.ADD,
                onClick = { onChangeMode(ToppingEditMode.ADD) },
                modifier = Modifier.weight(1f),
                iconResource = DesignSystemR.drawable.ic_add_round,
            )
        }
    }
}

/**
 * 영역 탭의 모드 토글 자리를 색상칩이 대신한다.
 * 고른 색과 굵기가 되돌리기 스택에 쌓이므로 값은 화면이 아니라 [ToppingEditState] 가 들고 있다.
 */
@Composable
private fun SegmentationBorderControls(
    state: ToppingEditState,
    onSelectColor: (Color) -> Unit,
    onChangeWidth: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = YGTheme.layout.padding.padding7

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 라벨과 바는 한 덩어리로 붙여야 해서 바깥 Column 의 간격을 타지 않도록 따로 감싼다
        Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
            Text(
                text = stringResource(R.string.topping_edit_border_width),
                style = YGTheme.typography.caption.c01M,
                color = YGAtomicColors.Gray.Gray700,
            )
            Spacer(modifier = Modifier.height(4.dp))
            BrushWidthSlider(
                value = state.borderWidth,
                onValueChange = onChangeWidth,
                valueRange = state.minBorderWidth..state.maxBorderWidth,
                isEnabled = !state.isLoading,
            )
        }

        BorderColorChipRow(
            selectedColor = state.selectedBorderColor,
            onSelectColor = onSelectColor,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = horizontalPadding),
        )
    }
}

private class ToppingEditStatePreviewParameterProvider : PreviewParameterProvider<ToppingEditState> {
    override val values: Sequence<ToppingEditState> = sequenceOf(
        ToppingEditState(),
        ToppingEditState(isSaving = true),
    )
}

@YGPreview
@Composable
private fun PreviewToppingEditScreen(
    @PreviewParameter(ToppingEditStatePreviewParameterProvider::class) state: ToppingEditState,
) = PreviewBox {
    ToppingEditScreen(
        state = state,
        onChangeTab = {},
        onChangeMode = {},
        onChangeBrushWidth = {},
        onAddStroke = {},
        onClickUndoArea = {},
        onClickRedoArea = {},
        onSelectBorderColor = {},
        onChangeBorderWidth = {},
        onClickUndoBorder = {},
        onClickRedoBorder = {},
        onClickDone = {},
        onClickBack = {},
        modifier = Modifier.fillMaxSize(),
    )
}
