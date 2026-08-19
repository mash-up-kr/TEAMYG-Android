package com.teamyg.parfait.feature.segmentation.impl.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.core.graphics.createBitmap
import com.teamyg.parfait.core.util.android.extension.toAndroidPath
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer

/**
 * Segmentation 결과에 사용자의 획을 반영해 테두리를 두르기 전 알맹이를 만든다.
 *
 * 마스크를 알파 채널로 다루는 게 핵심이다.
 * 1. segmentation 결과를 그려 그 알파를 시작 마스크로 삼는다
 * 2. [ToppingEditMode.ADD] 는 불투명하게, [ToppingEditMode.ERASE] 는 CLEAR 로 그려 마스크를 가감한다
 * 3. 마지막에 원본을 SRC_IN 으로 얹으면 마스크가 남은 자리에만 원본 픽셀이 채워진다
 *
 * 3번 덕분에 지웠던 영역을 다시 ADD 로 칠하면 원본 픽셀이 그대로 복원된다.
 * 다만 3번이 알파만 물려받고 색은 원본에서 다시 채우므로, [segmentationBitmap] 에 테두리가 구워진
 * 이미지를 넘기면 그 색이 원본 픽셀로 덮인다. 마스크로는 테두리를 두르기 전 알맹이를 넘겨야 한다.
 *
 * 결과는 언제나 [originBitmap] 크기이며, [segmentationBitmap] 크기가 달라도 늘려서 맞춘다.
 */
internal fun buildCutoutBitmap(
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
 * 알맹이 실루엣에서 떨어진 거리를 재 두고, 겹마다 정해진 거리까지를 그 색으로 칠해 테두리를 만든다.
 * 화면 미리보기와 같은 방식이라 여기서 본 모습이 그대로 파일에 남는다.
 *
 * 알맹이는 원본 자리 그대로 두고 테두리만 바깥으로 번지므로,
 * 원본 밖으로 나간 테두리는 캔버스 경계에서 잘린다.
 *
 * 겹의 굵기는 dp 라 [originPxPerDp] 로 원본 좌표계 굵기를 얻는다. 미리보기가 화면에 그릴 때 쓰는
 * 배율을 되짚은 값이므로, 화면에서 본 굵기가 그대로 파일에 남는다.
 */
internal fun Bitmap.withBorders(
    borderLayers: List<ToppingBorderLayer>,
    originPxPerDp: Float,
): Bitmap {
    if (borderLayers.isEmpty() || originPxPerDp <= 0f) return this

    val bordered = toOutlineDistanceField().buildBorderBitmap(
        targetWidth = width,
        targetHeight = height,
        bands = borderLayers.toBorderBands(originPxPerDp),
    ) ?: return this

    Canvas(bordered).drawBitmap(this, 0f, 0f, null)

    return bordered
}

/**
 * 투명한 여백을 걷어내고 실제로 보이는(알파가 있는) 픽셀의 최소 사각형만 남긴다.
 * 완전히 투명한 이미지처럼 자를 기준이 없으면 원본을 그대로 돌려준다.
 */
internal fun Bitmap.trimTransparentBounds(): Bitmap {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)

    var left = width
    var top = height
    var right = -1
    var bottom = -1

    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            if (pixels[rowOffset + x] ushr 24 != 0) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }

    if (right < left || bottom < top) return this

    return Bitmap.createBitmap(this, left, top, right - left + 1, bottom - top + 1)
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
