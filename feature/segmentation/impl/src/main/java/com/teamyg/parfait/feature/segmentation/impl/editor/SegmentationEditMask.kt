package com.teamyg.parfait.feature.segmentation.impl.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect

/**
 * Segmentation 결과에 사용자의 획을 반영해 최종 이미지를 만든다.
 *
 * 마스크를 알파 채널로 다루는 게 핵심이다.
 * 1. segmentation 결과를 그려 그 알파를 시작 마스크로 삼는다
 * 2. [SegmentationEditMode.ADD] 는 불투명하게, [SegmentationEditMode.ERASE] 는 CLEAR 로 그려 마스크를 가감한다
 * 3. 마지막에 원본을 SRC_IN 으로 얹으면 마스크가 남은 자리에만 원본 픽셀이 채워진다
 *
 * 3번 덕분에 지웠던 영역을 다시 ADD 로 칠하면 원본 픽셀이 그대로 복원된다.
 * 결과는 항상 [originBitmap] 크기이며, [segmentationBitmap] 크기가 달라도 늘려서 맞춘다.
 */
internal fun buildEditedBitmap(
    originBitmap: Bitmap,
    segmentationBitmap: Bitmap,
    strokes: List<SegmentationEditStroke>,
): Bitmap {
    val width = originBitmap.width
    val height = originBitmap.height
    val bounds = Rect(0, 0, width, height)

    val edited = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(edited)

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

    return edited
}

private fun strokePaint(stroke: SegmentationEditStroke): Paint = Paint().apply {
    isAntiAlias = true
    style = Paint.Style.STROKE
    strokeWidth = stroke.width
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    when (stroke.mode) {
        // 색은 무의미하다. SRC_IN 단계에서 원본으로 덮이므로 알파를 채우는 역할만 한다
        SegmentationEditMode.ADD -> color = Color.BLACK

        SegmentationEditMode.ERASE -> xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
}

/**
 * 점 하나짜리 획은 선으로 그려지지 않으므로, 제자리에 아주 짧은 선을 그어 점을 찍는다.
 */
private fun List<androidx.compose.ui.geometry.Offset>.toAndroidPath(): Path = Path().apply {
    val points = this@toAndroidPath
    val first = points.firstOrNull() ?: return@apply

    moveTo(first.x, first.y)
    if (points.size == 1) {
        lineTo(first.x, first.y)
        return@apply
    }
    points.drop(1).forEach { point -> lineTo(point.x, point.y) }
}
