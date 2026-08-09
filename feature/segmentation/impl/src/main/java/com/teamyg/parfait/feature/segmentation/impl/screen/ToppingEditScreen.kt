package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import com.teamyg.parfait.feature.segmentation.impl.component.BrushWidthSlider
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditMode
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditStroke
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditTab
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.ToppingEditState
import kotlin.math.roundToInt
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun ToppingEditScreen(
    state: ToppingEditState,
    onChangeTab: (ToppingEditTab) -> Unit,
    onChangeMode: (ToppingEditMode) -> Unit,
    onChangeBrushWidth: (Float) -> Unit,
    onAddStroke: (ToppingEditStroke) -> Unit,
    onClickUndo: () -> Unit,
    onClickRedo: () -> Unit,
    onClickDone: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // 되돌릴 대상인 획은 영역 탭에서만 그린다
        if (state.tab == ToppingEditTab.AREA) {
            ToppingEditHistoryActions(
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                onClickUndo = onClickUndo,
                onClickRedo = onClickRedo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(YGTheme.layout.padding.padding7),
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
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.height(23.dp))

        when (state.tab) {
            ToppingEditTab.AREA -> SegmentationAreaControls(
                state = state,
                onChangeMode = onChangeMode,
                onChangeBrushWidth = onChangeBrushWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = YGTheme.layout.padding.padding6,
                        start = YGTheme.layout.padding.padding7,
                        end = YGTheme.layout.padding.padding7,
                    ),
            )

            // Todo : 테두리 굵기/색 컨트롤 자리
            ToppingEditTab.BORDER -> Unit
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

@Composable
private fun ToppingEditHistoryActions(
    canUndo: Boolean,
    canRedo: Boolean,
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
            isEnabled = canUndo,
        )
        YGEditActionButton(
            iconResource = DesignSystemR.drawable.ic_arrow_right,
            contentDescription = stringResource(R.string.topping_edit_redo),
            onClick = onClickRedo,
            isEnabled = canRedo,
        )
    }
}

@Composable
private fun ToppingEditCanvas(
    state: ToppingEditState,
    onAddStroke: (ToppingEditStroke) -> Unit,
    modifier: Modifier = Modifier,
) {
    val originBitmap = state.originBitmap ?: return
    val segmentationBitmap = state.segmentationBitmap ?: return

    // 그리는 도중의 획. 매 포인터 이벤트마다 ViewModel 상태를 갱신하지 않도록 화면이 들고 있다가
    // 드래그가 끝날 때 한 번만 확정한다
    var drawingPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

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
            .pointerInput(originBitmap, state.mode, brushWidthPx) {
                val mapping = BitmapViewMapping.fitCenter(size, originBitmap.width, originBitmap.height)
                detectTapGestures(
                    onTap = { offset ->
                        drawingPoints = listOf(mapViewToBitmapFloat(offset, mapping))
                        commitStroke(mapping)
                    },
                )
            }.pointerInput(originBitmap, state.mode, brushWidthPx) {
                val mapping = BitmapViewMapping.fitCenter(size, originBitmap.width, originBitmap.height)
                detectDragGestures(
                    onDragStart = { offset -> drawingPoints = listOf(mapViewToBitmapFloat(offset, mapping)) },
                    onDrag = { change, _ ->
                        drawingPoints = drawingPoints + mapViewToBitmapFloat(change.position, mapping)
                    },
                    onDragEnd = { commitStroke(mapping) },
                    onDragCancel = { drawingPoints = emptyList() },
                )
            },
    ) {
        val mapping = BitmapViewMapping.fitCenter(size, originBitmap.width, originBitmap.height)
        val dstOffset = IntOffset(mapping.offsetX.roundToInt(), mapping.offsetY.roundToInt())
        val dstSize = IntSize(
            width = (originBitmap.width * mapping.scale).roundToInt(),
            height = (originBitmap.height * mapping.scale).roundToInt(),
        )

        // 알파 합성을 독립된 레이어에서 해야 Clear 가 화면 전체를 뚫지 않는다
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), Paint())

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

            canvas.restore()
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

@YGPreview
@Composable
private fun PreviewToppingEditScreen() = PreviewBox {
    ToppingEditScreen(
        state = ToppingEditState(),
        onChangeTab = {},
        onChangeMode = {},
        onChangeBrushWidth = {},
        onAddStroke = {},
        onClickUndo = {},
        onClickRedo = {},
        onClickDone = {},
        onClickBack = {},
        modifier = Modifier.fillMaxSize(),
    )
}
