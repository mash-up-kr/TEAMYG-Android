package com.teamyg.parfait.feature.segmentation.impl.screen

import android.graphics.Bitmap
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygeditactionbutton.YGEditActionButton
import com.teamyg.parfait.core.designsystem.component.ygeditbutton.YGEditButton
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarBackTitleClose
import com.teamyg.parfait.core.designsystem.component.ygslider.YGSlider
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToast
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.extension.toPath
import com.teamyg.parfait.feature.segmentation.impl.R
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditMode
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditStroke
import com.teamyg.parfait.feature.segmentation.impl.editor.UndoRedoStack
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.ToppingEditState
import kotlin.math.roundToInt
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/** 두 손가락으로 넓힐 수 있는 배율. 1배는 화면에 꽉 맞춘 처음 배치라 그보다 작게는 줄이지 않는다 */
private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 3f

private const val ERASED_AREA_ALPHA = 0.5f
private const val MASK_TINT_ALPHA = 0.5f

private const val BRUSH_PREVIEW_FILL_ALPHA = 0.5f
private val BRUSH_PREVIEW_BORDER_WIDTH = 1.dp

@Composable
internal fun ToppingEditScreen(
    state: ToppingEditState,
    onChangeMode: (ToppingEditMode) -> Unit,
    onChangeBrushWidth: (Float) -> Unit,
    onAddStroke: (ToppingEditStroke) -> Unit,
    onClickUndoArea: () -> Unit,
    onClickRedoArea: () -> Unit,
    onClickNext: () -> Unit,
    onClickBack: () -> Unit,
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
    toast: @Composable () -> Unit = {},
) {
    Box(modifier = modifier) {
        ToppingEditContent(
            state = state,
            onChangeMode = onChangeMode,
            onChangeBrushWidth = onChangeBrushWidth,
            onAddStroke = onAddStroke,
            onClickUndoArea = onClickUndoArea,
            onClickRedoArea = onClickRedoArea,
            onClickNext = onClickNext,
            onClickBack = onClickBack,
            onClickClose = onClickClose,
            toast = toast,
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
    onChangeMode: (ToppingEditMode) -> Unit,
    onChangeBrushWidth: (Float) -> Unit,
    onAddStroke: (ToppingEditStroke) -> Unit,
    onClickUndoArea: () -> Unit,
    onClickRedoArea: () -> Unit,
    onClickNext: () -> Unit,
    onClickBack: () -> Unit,
    onClickClose: () -> Unit,
    toast: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 붓 크기 미리보기는 슬라이더를 잡고 있는 동안만 띄운다
    var isAdjustingBrushWidth by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        YGFloatingBarBackTitleClose(
            title = stringResource(R.string.topping_edit_title),
            onBackClick = onClickBack,
            onCloseClick = onClickClose,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(YGTheme.layout.padding.padding4))

        ToppingEditBody(
            photo = {
                // 토스트가 화면 폭을 써야 해서 좌우 여백은 캔버스에만 준다
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = YGTheme.layout.padding.padding7),
                    ) {
                        // 상태를 통째로 넘기면 굵기 하나만 바뀌어도 캔버스가 함께 다시 그려지므로 쓰는 값만 넘긴다
                        val originBitmap = state.originBitmap
                        val segmentationBitmap = state.segmentationBitmap

                        when {
                            originBitmap == null || segmentationBitmap == null -> CircularProgressIndicator()

                            else -> ToppingEditCanvas(
                                originBitmap = originBitmap,
                                segmentationBitmap = segmentationBitmap,
                                strokes = state.strokes,
                                mode = state.mode,
                                brushWidthDp = state.brushWidthDp,
                                onAddStroke = onAddStroke,
                                isBrushPreviewVisible = isAdjustingBrushWidth,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        toast()
                    }
                }
            },
            brushLabel = {
                Text(
                    text = stringResource(R.string.topping_edit_brush_width),
                    style = YGTheme.typography.caption.c01M,
                    color = YGAtomicColors.Gray.Gray700,
                    modifier = Modifier.padding(
                        top = YGTheme.layout.gap.gap5,
                        start = YGTheme.layout.padding.padding7,
                        end = YGTheme.layout.padding.padding7,
                    ),
                )
            },
            controls = {
                SegmentationAreaControls(
                    mode = state.mode,
                    brushWidth = state.brushWidthDp,
                    brushWidthRange = state.minBrushWidthDp..state.maxBrushWidthDp,
                    isEnabled = !state.isLoading,
                    onChangeMode = onChangeMode,
                    onChangeBrushWidth = { width ->
                        isAdjustingBrushWidth = true
                        onChangeBrushWidth(width)
                    },
                    onChangeBrushWidthFinished = { isAdjustingBrushWidth = false },
                    modifier = Modifier.padding(
                        top = YGTheme.layout.gap.gap3,
                        start = YGTheme.layout.padding.padding7,
                        end = YGTheme.layout.padding.padding7,
                    ),
                )
            },
            historyActions = {
                ToppingEditHistoryActions(
                    history = state.areaHistory,
                    onClickUndo = onClickUndoArea,
                    onClickRedo = onClickRedoArea,
                    modifier = Modifier.padding(end = YGTheme.layout.padding.padding7),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        YGButton(
            text = stringResource(R.string.topping_edit_next),
            buttonType = YGButtonType.Large,
            isEnabled = !state.isLoading,
            onClick = onClickNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = YGTheme.layout.padding.padding7,
                    end = YGTheme.layout.padding.padding7,
                    top = YGTheme.layout.gap.gap3,
                ),
        )
    }
}

/**
 * 되돌리기·다시하기가 사진 아랫변에 겹쳐도 전체가 눌리도록 사진과 편집 영역을 한 레이아웃에 놓는다.
 * 슬롯마다 최상위 요소는 하나여야 한다.
 */
@Composable
private fun ToppingEditBody(
    photo: @Composable () -> Unit,
    brushLabel: @Composable () -> Unit,
    controls: @Composable () -> Unit,
    historyActions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            photo()
            brushLabel()
            controls()
            historyActions()
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val rowConstraints = Constraints(minWidth = width, maxWidth = width, maxHeight = height)

        val label = measurables[1].measure(rowConstraints)
        val controlsPlaceable = measurables[2].measure(rowConstraints)
        val actions = measurables[3].measure(Constraints(maxWidth = width, maxHeight = height))

        val photoHeight = (height - label.height - controlsPlaceable.height).coerceAtLeast(0)
        val photoPlaceable = measurables[0].measure(Constraints.fixed(width, photoHeight))
        val labelBottom = photoHeight + label.height

        layout(width, height) {
            photoPlaceable.place(0, 0)
            label.place(0, photoHeight)
            controlsPlaceable.place(0, labelBottom)
            actions.place(width - actions.width, (labelBottom - actions.height).coerceAtLeast(0))
        }
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

/** 되돌릴 수 있는지만 보므로 [history] 가 무엇을 쌓는지는 알 필요가 없다 */
@Composable
private fun ToppingEditHistoryActions(
    history: UndoRedoStack<*>,
    onClickUndo: () -> Unit,
    onClickRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
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
    originBitmap: Bitmap,
    segmentationBitmap: Bitmap,
    strokes: List<ToppingEditStroke>,
    mode: ToppingEditMode,
    brushWidthDp: Float,
    onAddStroke: (ToppingEditStroke) -> Unit,
    isBrushPreviewVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    // 그리는 도중의 획. 매 포인터 이벤트마다 ViewModel 상태를 갱신하지 않도록 화면이 들고 있다가
    // 드래그가 끝날 때 한 번만 확정한다.
    // 스냅샷 리스트라 점을 덧붙여도 리스트를 통째로 베끼지 않으면서 다시 그리기는 그대로 걸린다
    val drawingPoints = remember { mutableStateListOf<Offset>() }

    // 사진이 바뀌면 처음 배치로 되돌린다
    var zoom by remember(originBitmap) { mutableFloatStateOf(MIN_ZOOM) }
    var pan by remember(originBitmap) { mutableStateOf(Offset.Zero) }

    val originImage = remember(originBitmap) { originBitmap.asImageBitmap() }
    val segmentationImage = remember(segmentationBitmap) { segmentationBitmap.asImageBitmap() }

    val brushWidthPx = with(LocalDensity.current) { brushWidthDp.dp.toPx() }

    // 확정된 획은 더 바뀌지 않으므로 프레임마다 다시 이을 필요가 없다.
    // 원본 좌표 그대로 담아 두고 그릴 때 화면 배율만 태운다
    val strokePaths = remember(strokes) { strokes.map { stroke -> stroke.points.toPath() } }

    // 획을 확정하는 시점의 값만 있으면 되므로, 값이 바뀔 때마다 제스처 감지기를 다시 세우지 않는다
    val currentMode by rememberUpdatedState(mode)
    val currentBrushWidthPx by rememberUpdatedState(brushWidthPx)
    val currentOnAddStroke by rememberUpdatedState(onAddStroke)

    fun commitStroke(mapping: BitmapViewMapping) {
        if (drawingPoints.isNotEmpty()) {
            // 확정한 획이 뒤이어 비워질 목록을 그대로 들고 가지 않도록 여기서 사본을 뜬다.
            // 붓 굵기는 화면 기준이라 확대 배율을 걷어내야 원본 좌표계 굵기가 된다
            val stroke = ToppingEditStroke(
                mode = currentMode,
                points = drawingPoints.toList(),
                width = currentBrushWidthPx / mapping.scale,
            )
            currentOnAddStroke(stroke)
        }
        drawingPoints.clear()
    }

    Canvas(
        modifier = modifier
            // 그리기는 레이아웃 경계를 저절로 지키지 않는다. 확대한 사진이 편집 영역을 넘어
            // 아래 조작부까지 번지지 않도록 처음 받은 자리에서 잘라낸다
            .clipToBounds()
            .pointerInput(originBitmap) {
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
                    drawingPoints.clear()
                    drawingPoints.add(mapViewToBitmapFloat(down.position, currentMapping()))

                    do {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }

                        if (pressedChanges.size >= 2) {
                            if (!isTransforming) {
                                // 두 번째 손가락이 닿는 순간 그리던 획은 버린다
                                isTransforming = true
                                drawingPoints.clear()
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
                                drawingPoints.add(mapViewToBitmapFloat(change.position, currentMapping()))
                                change.consume()
                            }
                        }
                    } while (event.changes.any { change -> change.pressed })

                    if (isTransforming) {
                        drawingPoints.clear()
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

                // 획은 원본 좌표로 담겨 있으므로 화면 자리로 옮겨 놓고 그린다. 굵기도 함께 늘어난다
                withTransform({
                    translate(mapping.offsetX, mapping.offsetY)
                    scale(mapping.scale, mapping.scale, pivot = Offset.Zero)
                }) {
                    strokes.forEachIndexed { index, stroke ->
                        drawEditStroke(strokePaths[index], stroke.width, stroke.mode)
                    }
                    if (drawingPoints.isNotEmpty()) {
                        drawEditStroke(drawingPoints.toPath(), brushWidthPx / mapping.scale, mode)
                    }
                }

                // 마스크가 남은 자리에만 원본 픽셀을 채운다. ADD 로 칠한 곳이 원본으로 복원되는 지점
                drawImage(
                    image = originImage,
                    dstOffset = dstOffset,
                    dstSize = dstSize,
                    blendMode = BlendMode.SrcIn,
                )

                // 남는 영역을 색으로 알아보게 한다. SrcAtop 은 알파가 있는 자리에만 닿아서 마스크 경계를 그대로 탄다
                drawRect(
                    color = YGAtomicColors.Cherry.Cherry500.copy(alpha = MASK_TINT_ALPHA),
                    topLeft = Offset(dstOffset.x.toFloat(), dstOffset.y.toFloat()),
                    size = Size(dstSize.width.toFloat(), dstSize.height.toFloat()),
                    blendMode = BlendMode.SrcAtop,
                )
            }

            canvas.restore()
        }

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
    path: Path,
    width: Float,
    mode: ToppingEditMode,
) {
    drawPath(
        path = path,
        color = Color.Black,
        style = Stroke(
            width = width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
        // 색은 무의미하다. SrcIn 단계에서 원본으로 덮이므로 알파를 채우고 지우는 역할만 한다
        blendMode = when (mode) {
            ToppingEditMode.ADD -> BlendMode.SrcOver
            ToppingEditMode.ERASE -> BlendMode.Clear
        },
    )
}

@Composable
private fun SegmentationAreaControls(
    mode: ToppingEditMode,
    brushWidth: Float,
    brushWidthRange: ClosedFloatingPointRange<Float>,
    isEnabled: Boolean,
    onChangeMode: (ToppingEditMode) -> Unit,
    onChangeBrushWidth: (Float) -> Unit,
    onChangeBrushWidthFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
    ) {
        YGSlider(
            value = brushWidth,
            onValueChange = onChangeBrushWidth,
            onValueChangeFinished = onChangeBrushWidthFinished,
            valueRange = brushWidthRange,
            isEnabled = isEnabled,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1)) {
            YGEditButton(
                text = stringResource(R.string.topping_edit_area_erase),
                isSelected = mode == ToppingEditMode.ERASE,
                onClick = { onChangeMode(ToppingEditMode.ERASE) },
                modifier = Modifier.weight(1f),
                iconResource = DesignSystemR.drawable.ic_minus_round,
            )
            YGEditButton(
                text = stringResource(R.string.topping_edit_area_add),
                isSelected = mode == ToppingEditMode.ADD,
                onClick = { onChangeMode(ToppingEditMode.ADD) },
                modifier = Modifier.weight(1f),
                iconResource = DesignSystemR.drawable.ic_add_round,
            )
        }
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
        onChangeMode = {},
        onChangeBrushWidth = {},
        onAddStroke = {},
        onClickUndoArea = {},
        onClickRedoArea = {},
        onClickNext = {},
        onClickBack = {},
        onClickClose = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@YGPreview
@Composable
private fun PreviewToppingEditScreenDetectionFailed() = PreviewBox {
    ToppingEditScreen(
        state = ToppingEditState(),
        onChangeMode = {},
        onChangeBrushWidth = {},
        onAddStroke = {},
        onClickUndoArea = {},
        onClickRedoArea = {},
        onClickNext = {},
        onClickBack = {},
        onClickClose = {},
        toast = {
            YGToast(
                type = YGToastType.Edit(stringResource(R.string.topping_edit_detection_failed)),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}
