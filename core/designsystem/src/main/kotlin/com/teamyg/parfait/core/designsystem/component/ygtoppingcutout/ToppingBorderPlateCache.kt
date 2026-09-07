package com.teamyg.parfait.core.designsystem.component.ygtoppingcutout

import androidx.compose.ui.graphics.ImageBitmap
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline

/** 항목 크기가 알맹이 + 사방 굵기라 굵기에 상한이 없는 한 **칸 수만 묶이고 총량은 안 묶인다** */
private const val PLATE_CACHE_ENTRIES = 32

private const val LOAD_FACTOR = 0.75f

/**
 * 알맹이와 여백을 함께 담은 띠 한 장. 지금 알맹이와 크기가 다를 수 있어, [padding] 을 포함한 판
 * 전체를 실제 알맹이 크기에 맞춰 늘려 그린다.
 *
 * @param subjectLongSide 이 판을 만들 때 겨냥한 알맹이의 긴 변. 굵기가 판에 구워져 있어 다른
 *   크기로 늘려 그리면 화면상 굵기가 그 비율만큼 틀어진다
 */
internal data class ToppingBorderPlate(
    val image: ImageBitmap,
    val padding: Int,
    val subjectLongSide: Int,
)

/** 늘려 그려도 굵기 차이가 눈에 안 띄는 배율의 상한 */
private const val PLATE_REUSE_RATIO_LIMIT = 1.25f

internal fun ToppingBorderPlate.fitsSubject(subjectLongSide: Int): Boolean {
    if (this.subjectLongSide <= 0 || subjectLongSide <= 0) return false

    val ratio = subjectLongSide.toFloat() / this.subjectLongSide
    return ratio <= PLATE_REUSE_RATIO_LIMIT && ratio >= 1f / PLATE_REUSE_RATIO_LIMIT
}

/**
 * **표시 크기는 안 넣는다** — 넣으면 핀치 한 번에 항목이 쏟아지고 화면 전환마다 미스가 난다.
 * 어긋난 정도는 꺼낸 쪽이 [ToppingBorderPlate.fitsSubject] 로 잰다. **비율은 넣는다** — 늘려
 * 그리는 배율이 가로 하나뿐이라 비율이 다른 판은 세로가 어긋난다.
 *
 * [ToppingOutline] 은 동등성을 재정의하지 않으므로 **같은 인스턴스**일 때만 같은 열쇠가 된다.
 */
private data class PlateKey(
    val outline: ToppingOutline,
    val outsetPx: Float,
    val aspectRatio: Float,
)

/**
 * 컴포저블이 다시 만들어질 때 판까지 다시 만들면 그동안 테두리를 안 그려 깜빡인다.
 *
 * 접근 순서 갱신이 곧 쓰기라 모든 접근을 이 맵 자신에 대해 동기화한다.
 */
private val plateCache = object : LinkedHashMap<PlateKey, ToppingBorderPlate>(
    PLATE_CACHE_ENTRIES,
    LOAD_FACTOR,
    true,
) {
    override fun removeEldestEntry(eldest: Map.Entry<PlateKey, ToppingBorderPlate>): Boolean =
        size > PLATE_CACHE_ENTRIES
}

/** 크기가 안 맞는 판도 돌려준다 — 어긋난 정도는 [ToppingBorderPlate.fitsSubject] 로 잰다 */
internal fun cachedToppingBorderPlate(
    outline: ToppingOutline,
    outsetPx: Float,
    aspectRatio: Float,
): ToppingBorderPlate? = synchronized(plateCache) { plateCache[PlateKey(outline, outsetPx, aspectRatio)] }

internal fun cacheToppingBorderPlate(
    outline: ToppingOutline,
    outsetPx: Float,
    aspectRatio: Float,
    plate: ToppingBorderPlate,
) {
    synchronized(plateCache) { plateCache[PlateKey(outline, outsetPx, aspectRatio)] = plate }
}

internal fun clearToppingBorderPlates() {
    synchronized(plateCache) { plateCache.clear() }
}
