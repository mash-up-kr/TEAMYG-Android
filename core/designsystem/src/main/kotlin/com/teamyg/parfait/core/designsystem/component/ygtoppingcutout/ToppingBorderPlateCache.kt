package com.teamyg.parfait.core.designsystem.component.ygtoppingcutout

import androidx.compose.ui.graphics.ImageBitmap
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline

/**
 * 캔버스 하나가 띄우는 토핑 수를 덮는 상한.
 *
 * 항목 하나의 크기는 판 넓이 × 1바이트(`ALPHA_8`)이고, 판은 알맹이에 사방 여백(굵기 + 1px)을
 * 더한 것이다. 굵기가 서버에서 오는 값이라 상한이 없으므로 **총량에 고정 상한을 못 건다.**
 */
private const val PLATE_CACHE_ENTRIES = 32

private const val LOAD_FACTOR = 0.75f

/**
 * 알맹이와 여백을 함께 담은 띠 한 장.
 *
 * 판은 해상도 상한이나 캐시 재사용 때문에 지금 알맹이와 크기가 다를 수 있으므로, [padding] 을
 * 포함한 판 전체를 그릴 때 실제 알맹이 크기에 맞춰 늘린다.
 *
 * @param subjectLongSide 이 판이 겨냥한 알맹이의 긴 변. 굵기가 판에 구워져 있어 다른 크기에서
 *   늘려 그리면 화면상 굵기가 그 비율만큼 틀어지므로, 얼마나 어긋났는지 재려면 이 값이 필요하다
 */
internal data class ToppingBorderPlate(
    val image: ImageBitmap,
    val padding: Int,
    val subjectLongSide: Int,
)

/** 늘려 그려도 굵기 차이가 눈에 안 띄는 배율의 상한 */
private const val PLATE_REUSE_RATIO_LIMIT = 1.25f

/** [subjectLongSide] 크기로 늘려 그렸을 때 굵기가 [PLATE_REUSE_RATIO_LIMIT] 안에 드는가 */
internal fun ToppingBorderPlate.fitsSubject(subjectLongSide: Int): Boolean {
    if (this.subjectLongSide <= 0 || subjectLongSide <= 0) return false

    val ratio = subjectLongSide.toFloat() / this.subjectLongSide
    return ratio <= PLATE_REUSE_RATIO_LIMIT && ratio >= 1f / PLATE_REUSE_RATIO_LIMIT
}

/**
 * **표시 크기를 열쇠에 넣지 않는다.** 크기까지 맞아야 꺼낼 수 있으면 핀치 한 번에 항목이 쏟아지고
 * 화면 전환마다 미스가 난다. 대신 얼마나 어긋났는지를 꺼낸 쪽이 [fitsSubject] 로 잰다. 반면 비율은
 * 넣는다 — 판을 늘려 그리는 배율이 가로 하나뿐이라, 비율이 다른 판은 세로가 어긋난 채로 그려진다.
 *
 * [ToppingOutline] 은 동등성을 재정의하지 않으므로 **같은 인스턴스**일 때만 같은 열쇠가 된다.
 */
private data class PlateKey(
    val outline: ToppingOutline,
    val outsetPx: Float,
    val aspectRatio: Float,
)

/**
 * 판을 컴포지션 밖에 남긴다. 컴포저블이 다시 만들어질 때 판까지 다시 만들면 그동안 테두리를 안
 * 그려 깜빡인다.
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

/** 크기가 안 맞는 판도 돌려준다 — 얼마나 어긋났는지는 [fitsSubject] 로 부르는 쪽이 잰다 */
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

/** 메모리 압박이나 테스트에서 판을 비우는 수단 */
internal fun clearToppingBorderPlates() {
    synchronized(plateCache) { plateCache.clear() }
}
