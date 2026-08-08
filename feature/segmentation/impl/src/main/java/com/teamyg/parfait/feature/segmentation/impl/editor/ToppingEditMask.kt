package com.teamyg.parfait.feature.segmentation.impl.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap

/**
 * Segmentation 결과에 사용자의 획을 반영해 최종 이미지를 만든다.
 *
 * 마스크를 알파 채널로 다루는 게 핵심이다.
 * 1. segmentation 결과를 그려 그 알파를 시작 마스크로 삼는다
 * 2. [ToppingEditMode.ADD] 는 불투명하게, [ToppingEditMode.ERASE] 는 CLEAR 로 그려 마스크를 가감한다
 * 3. 마지막에 원본을 SRC_IN 으로 얹으면 마스크가 남은 자리에만 원본 픽셀이 채워진다
 *
 * 3번 덕분에 지웠던 영역을 다시 ADD 로 칠하면 원본 픽셀이 그대로 복원된다.
 *
 * [borderStrokes] 가 있으면 잘라낸 결과를 겹겹이 두르되, 알맹이는 원본 자리를 그대로 지킨다.
 * 결과는 언제나 [originBitmap] 크기이며, [segmentationBitmap] 크기가 달라도 늘려서 맞춘다.
 */
internal fun buildEditedBitmap(
    originBitmap: Bitmap,
    segmentationBitmap: Bitmap,
    strokes: List<ToppingEditStroke>,
    borderStrokes: List<ToppingBorderStroke> = emptyList(),
): Bitmap {
    val cutout = buildCutoutBitmap(originBitmap, segmentationBitmap, strokes)
    if (borderStrokes.isEmpty()) return cutout

    return cutout.withBorders(borderStrokes)
}

/** 테두리를 두르기 전 알맹이 */
private fun buildCutoutBitmap(
    originBitmap: Bitmap,
    segmentationBitmap: Bitmap,
    strokes: List<ToppingEditStroke>,
): Bitmap {
    val bounds = Rect(0, 0, originBitmap.width, originBitmap.height)

    val cutout = createBitmap(originBitmap.width, originBitmap.height)
    val canvas = Canvas(cutout)

    canvas.drawBitmap(segmentationBitmap, null, bounds, null)

    strokes.forEach { stroke ->
        canvas.drawPath(stroke.points.toAndroidPath(), strokePaint(stroke))
    }

    canvas.drawBitmap(
        originBitmap,
        null,
        bounds,
        Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN) },
    )

    return cutout
}

/**
 * 알파 마스크를 실제로 팽창시키는 대신, 알맹이를 반지름만큼 떨어진 자리에 빙 둘러 찍고
 * 겹친 자국을 겹 색으로 물들여 굵은 윤곽을 만든다. 화면 미리보기와 같은 방식이다.
 *
 * 알맹이는 원본 자리 그대로 두고 테두리만 바깥으로 번지므로,
 * 원본 밖으로 나간 테두리는 캔버스 경계에서 잘린다.
 */
private fun Bitmap.withBorders(borderStrokes: List<ToppingBorderStroke>): Bitmap {
    val destination = RectF(0f, 0f, width.toFloat(), height.toFloat())

    val bordered = createBitmap(width, height)
    val canvas = Canvas(bordered)

    // 겹이 안쪽부터 쌓여 있으므로 그릴 때는 가장 바깥부터 깔아야 안쪽 겹이 위에 남는다
    borderStrokes.withOutsets().asReversed().forEach { (stroke, strokeOutset) ->
        val paint = borderPaint(stroke.color.toArgb())
        outlineOffsets(strokeOutset).forEach { offset ->
            val shifted = RectF(destination).apply { offset(offset.x, offset.y) }
            canvas.drawBitmap(this, null, shifted, paint)
        }
    }

    canvas.drawBitmap(this, null, destination, borderPaint(colorArgb = null))

    return bordered
}

/** 소수점 자리로 밀어 찍으므로 보간을 켠다. [colorArgb] 가 있으면 알파는 두고 색만 그 색으로 갈아끼운다 */
private fun borderPaint(colorArgb: Int?): Paint = Paint().apply {
    isFilterBitmap = true
    isAntiAlias = true
    colorArgb?.let { colorFilter = PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN) }
}

private fun strokePaint(stroke: ToppingEditStroke): Paint = Paint().apply {
    isAntiAlias = true
    style = Paint.Style.STROKE
    strokeWidth = stroke.width
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    when (stroke.mode) {
        // 색은 무의미하다. SRC_IN 단계에서 원본으로 덮이므로 알파를 채우는 역할만 한다
        ToppingEditMode.ADD -> color = Color.BLACK

        ToppingEditMode.ERASE -> xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
}

/**
 * 점 하나짜리 획은 선으로 그려지지 않으므로, 제자리에 아주 짧은 선을 그어 점을 찍는다.
 */
private fun List<Offset>.toAndroidPath(): Path = Path().apply {
    val points = this@toAndroidPath
    val first = points.firstOrNull() ?: return@apply

    moveTo(first.x, first.y)
    if (points.size == 1) {
        lineTo(first.x, first.y)
        return@apply
    }
    points.drop(1).forEach { point -> lineTo(point.x, point.y) }
}
