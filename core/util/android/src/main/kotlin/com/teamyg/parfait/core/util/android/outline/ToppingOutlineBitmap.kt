package com.teamyg.parfait.core.util.android.outline

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.teamyg.parfait.core.util.jvm.outline.ToppingBorderBand
import com.teamyg.parfait.core.util.jvm.outline.ToppingBorderTarget
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val ALPHA_SHIFT = 24

/**
 * 알파가 남아 있는 자리를 실루엣으로 보고 거리를 잰다.
 *
 * @param fieldLongSide 거리를 잴 때 긴 변을 이 길이까지만 쓴다. 테두리 경계는 굵기만큼 완만한
 *   곡선이라 촘촘히 재도 모양이 달라지지 않는데, 원본 해상도로 재면 사진 크기에 비례해 시간과
 *   메모리만 늘어난다
 */
fun Bitmap.toToppingOutline(fieldLongSide: Int): ToppingOutline {
    val fieldScale = min(1f, fieldLongSide.toFloat() / max(width, height))
    val fieldWidth = max(1, (width * fieldScale).roundToInt())
    val fieldHeight = max(1, (height * fieldScale).roundToInt())

    val source = if (fieldWidth == width && fieldHeight == height) this else scale(fieldWidth, fieldHeight)
    val pixels = IntArray(fieldWidth * fieldHeight)
    source.getPixels(pixels, 0, fieldWidth, 0, 0, fieldWidth, fieldHeight)
    if (source !== this) source.recycle()

    return ToppingOutline.of(fieldWidth, fieldHeight) { x, y ->
        pixels[y * fieldWidth + x] ushr ALPHA_SHIFT
    }
}

/** 색을 태우지 않은 띠. 그리는 쪽이 `ColorFilter` 로 물들인다 */
fun ToppingOutline.toBorderAlphaBitmap(
    target: ToppingBorderTarget,
    outsetPx: Float,
    shouldContinue: () -> Boolean = { true },
): Bitmap? {
    val alpha = buildBorderAlpha(target, outsetPx, shouldContinue) ?: return null
    return createBitmap(target.width, target.height, Bitmap.Config.ALPHA_8).apply {
        copyPixelsFromBuffer(ByteBuffer.wrap(alpha))
    }
}

/** 색까지 태운 띠. 겹이 여럿이거나 알파 판이 안 통하는 자리가 쓴다 */
fun ToppingOutline.toBorderArgbBitmap(
    target: ToppingBorderTarget,
    bands: List<ToppingBorderBand>,
    shouldContinue: () -> Boolean = { true },
): Bitmap? {
    val pixels = buildBorderPixels(target, bands, shouldContinue) ?: return null
    return createBitmap(target.width, target.height)
        .apply { setPixels(pixels, 0, target.width, 0, 0, target.width, target.height) }
}
