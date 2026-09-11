package com.teamyg.parfait.core.designsystem.component.ygtoppingcutout

import androidx.compose.ui.graphics.ImageBitmap
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline

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
 * 컴포저블이 다시 만들어질 때 판까지 다시 만들면 그동안 테두리를 안 그려 깜빡인다. 프로세스
 * 전역이고 비우는 주체가 없다(`synthesis/open-questions.md` OQ-P-317).
 *
 * 같은 토핑을 크기가 크게 다른 두 화면이 그리면 판을 서로 덮어써 깜빡이므로, 열쇠마다 크기별로 몇 장 둔다.
 */
internal object ToppingBorderPlateCache {
    /** 항목 크기가 알맹이 + 사방 굵기라 굵기에 상한이 없는 한 **칸 수만 묶이고 총량은 안 묶인다** */
    private const val MAX_ENTRIES = 32

    /** 한 토핑을 서로 다른 크기로 그리는 두 화면 + 드래그 중간 크기 한 장 */
    private const val PLATES_PER_KEY = 3

    private const val LOAD_FACTOR = 0.75f

    /** 접근 순서 갱신이 곧 쓰기라 모든 접근을 [entries] 자신에 대해 동기화한다 */
    private val entries =
        object : LinkedHashMap<PlateKey, ArrayDeque<ToppingBorderPlate>>(MAX_ENTRIES, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: Map.Entry<PlateKey, ArrayDeque<ToppingBorderPlate>>) =
                size > MAX_ENTRIES
        }

    /** [subjectLongSide] 가 있으면 그 크기에 맞는 판만 찾는다 — 없으면 크기와 무관하게 가장 최근 판 */
    fun get(
        outline: ToppingOutline,
        outsetPx: Float,
        aspectRatio: Float,
        subjectLongSide: Int? = null,
    ): ToppingBorderPlate? {
        synchronized(entries) {
            val shelf = entries[PlateKey(outline, outsetPx, aspectRatio)] ?: return null
            val index = if (subjectLongSide == null) {
                if (shelf.isEmpty()) -1 else 0
            } else {
                shelf.indexOfFirst { plate -> plate.fitsSubject(subjectLongSide) }
            }
            if (index < 0) return null

            val found = shelf.removeAt(index)
            shelf.addFirst(found)
            return found
        }
    }

    fun put(
        outline: ToppingOutline,
        outsetPx: Float,
        aspectRatio: Float,
        plate: ToppingBorderPlate,
    ) {
        synchronized(entries) {
            val shelf = entries.getOrPut(PlateKey(outline, outsetPx, aspectRatio)) { ArrayDeque() }

            // 드래그 중 이어서 만든 판이 선반을 채우지 않게 비슷한 크기 판은 대체한다
            shelf.removeAll { stored -> stored.fitsSubject(plate.subjectLongSide) }
            shelf.addFirst(plate)
            while (shelf.size > PLATES_PER_KEY) shelf.removeLast()
        }
    }

    fun clear() {
        synchronized(entries) { entries.clear() }
    }
}

/**
 * **표시 크기는 안 넣는다** — 넣으면 핀치 한 번에 항목이 쏟아지고 화면 전환마다 미스가 난다.
 * 크기가 다른 판은 한 열쇠의 선반에 함께 두고, 어긋난 정도는 꺼낸 쪽이 [ToppingBorderPlate.fitsSubject] 로
 * 잰다. **비율은 넣는다** — 늘려 그리는 배율이 가로 하나뿐이라 비율이 다른 판은 세로가 어긋난다.
 *
 * [ToppingOutline] 은 동등성을 재정의하지 않으므로 **같은 인스턴스**일 때만 같은 열쇠가 된다.
 */
private data class PlateKey(
    val outline: ToppingOutline,
    val outsetPx: Float,
    val aspectRatio: Float,
)
