package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.editor.SegmentationEditMode
import com.teamyg.parfait.feature.segmentation.impl.editor.SegmentationEditStroke
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationEditState
import kotlin.math.roundToInt

/** 잘려나간 영역도 어렴풋이 보여야 어디를 되살릴지 판단할 수 있다 */
private const val REMOVED_AREA_ALPHA = 0.25f

@Composable
internal fun SegmentationEditScreen(
    state: SegmentationEditState,
    onChangeMode: (SegmentationEditMode) -> Unit,
    onChangeBrushWidth: (Float) -> Unit,
    onAddStroke: (SegmentationEditStroke) -> Unit,
    onClickUndo: () -> Unit,
    onClickRedo: () -> Unit,
    onClickReset: () -> Unit,
    onClickDone: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val originBitmap = state.originBitmap
            val segmentationBitmap = state.segmentationBitmap

            if (originBitmap == null || segmentationBitmap == null) {
                CircularProgressIndicator()
            } else {
                SegmentationEditCanvas(
                    state = state,
                    onAddStroke = onAddStroke,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        SegmentationEditControls(
            state = state,
            onChangeMode = onChangeMode,
            onChangeBrushWidth = onChangeBrushWidth,
            onClickUndo = onClickUndo,
            onClickRedo = onClickRedo,
            onClickReset = onClickReset,
            onClickDone = onClickDone,
            onClickBack = onClickBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Composable
private fun SegmentationEditCanvas(
    state: SegmentationEditState,
    onAddStroke: (SegmentationEditStroke) -> Unit,
    modifier: Modifier = Modifier,
) {
    val originBitmap = state.originBitmap ?: return
    val segmentationBitmap = state.segmentationBitmap ?: return

    // 그리는 도중의 획. 매 포인터 이벤트마다 ViewModel 상태를 갱신하지 않도록 화면이 들고 있다가
    // 드래그가 끝날 때 한 번만 확정한다
    var drawingPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val originImage = remember(originBitmap) { originBitmap.asImageBitmap() }
    val segmentationImage = remember(segmentationBitmap) { segmentationBitmap.asImageBitmap() }

    fun commitStroke() {
        val points = drawingPoints
        drawingPoints = emptyList()
        if (points.isEmpty()) return
        onAddStroke(
            SegmentationEditStroke(
                mode = state.mode,
                points = points,
                width = state.brushWidth,
            ),
        )
    }

    Canvas(
        modifier = modifier
            .pointerInput(originBitmap, state.mode, state.brushWidth) {
                val mapping = viewMapping(size, originBitmap.width, originBitmap.height)
                detectTapGestures(
                    onTap = { offset ->
                        drawingPoints = listOf(mapViewToBitmapFloat(offset, mapping))
                        commitStroke()
                    },
                )
            }.pointerInput(originBitmap, state.mode, state.brushWidth) {
                val mapping = viewMapping(size, originBitmap.width, originBitmap.height)
                detectDragGestures(
                    onDragStart = { offset -> drawingPoints = listOf(mapViewToBitmapFloat(offset, mapping)) },
                    onDrag = { change, _ ->
                        drawingPoints = drawingPoints + mapViewToBitmapFloat(change.position, mapping)
                    },
                    onDragEnd = { commitStroke() },
                    onDragCancel = { drawingPoints = emptyList() },
                )
            },
    ) {
        val mapping = bitmapToViewMapping(size, originBitmap.width, originBitmap.height)
        val dstOffset = IntOffset(mapping.offsetX.roundToInt(), mapping.offsetY.roundToInt())
        val dstSize = IntSize(
            width = (originBitmap.width * mapping.scale).roundToInt(),
            height = (originBitmap.height * mapping.scale).roundToInt(),
        )

        drawImage(
            image = originImage,
            dstOffset = dstOffset,
            dstSize = dstSize,
            alpha = REMOVED_AREA_ALPHA,
        )

        // 알파 합성을 독립된 레이어에서 해야 아래에 깔아둔 흐린 원본이 지워지지 않는다
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), Paint())

            drawImage(image = segmentationImage, dstOffset = dstOffset, dstSize = dstSize)

            val strokes = state.strokes + drawingStroke(drawingPoints, state)
            strokes.forEach { stroke -> drawEditStroke(stroke, mapping) }

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

private fun drawingStroke(
    points: List<Offset>,
    state: SegmentationEditState,
): List<SegmentationEditStroke> = if (points.isEmpty()) {
    emptyList()
} else {
    listOf(SegmentationEditStroke(mode = state.mode, points = points, width = state.brushWidth))
}

private fun DrawScope.drawEditStroke(
    stroke: SegmentationEditStroke,
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
            SegmentationEditMode.ADD -> BlendMode.SrcOver
            SegmentationEditMode.ERASE -> BlendMode.Clear
        },
    )
}

private fun viewMapping(
    size: IntSize,
    bitmapWidth: Int,
    bitmapHeight: Int,
): BitmapViewMapping = bitmapToViewMapping(
    viewSize = Size(size.width.toFloat(), size.height.toFloat()),
    bitmapWidth = bitmapWidth,
    bitmapHeight = bitmapHeight,
)

@Composable
private fun SegmentationEditControls(
    state: SegmentationEditState,
    onChangeMode: (SegmentationEditMode) -> Unit,
    onChangeBrushWidth: (Float) -> Unit,
    onClickUndo: () -> Unit,
    onClickRedo: () -> Unit,
    onClickReset: () -> Unit,
    onClickDone: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 지우기는 알파를 비워 투명하게, 채우기는 원본 픽셀을 되살린다
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.mode == SegmentationEditMode.ERASE,
                onClick = { onChangeMode(SegmentationEditMode.ERASE) },
                label = { Text("영역 지우기") }, // Todo : core:ui 에 string resource 로 분리
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = state.mode == SegmentationEditMode.ADD,
                onClick = { onChangeMode(SegmentationEditMode.ADD) },
                label = { Text("영역 채우기") }, // Todo : core:ui 에 string resource 로 분리
                modifier = Modifier.weight(1f),
            )
        }

        Slider(
            value = state.brushWidth,
            onValueChange = onChangeBrushWidth,
            valueRange = state.minBrushWidth..state.maxBrushWidth,
            enabled = !state.isLoading,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onClickUndo, enabled = state.canUndo) {
                Text("실행 취소")
            }
            OutlinedButton(onClick = onClickRedo, enabled = state.canRedo) {
                Text("다시 실행")
            }
            OutlinedButton(onClick = onClickReset, enabled = state.canUndo) {
                Text("초기화")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onClickBack) {
                Text("뒤로")
            }
            Button(
                onClick = onClickDone,
                enabled = !state.isLoading && !state.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text("완료")
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewSegmentationEditScreen() = PreviewBox {
    SegmentationEditScreen(
        state = SegmentationEditState(),
        onChangeMode = {},
        onChangeBrushWidth = {},
        onAddStroke = {},
        onClickUndo = {},
        onClickRedo = {},
        onClickReset = {},
        onClickDone = {},
        onClickBack = {},
        modifier = Modifier.fillMaxSize(),
    )
}
