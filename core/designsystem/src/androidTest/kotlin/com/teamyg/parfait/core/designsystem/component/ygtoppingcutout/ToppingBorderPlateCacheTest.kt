package com.teamyg.parfait.core.designsystem.component.ygtoppingcutout

import androidx.compose.ui.graphics.ImageBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ToppingBorderPlateCacheTest {
    private val outline = ToppingOutline.of(OUTLINE_SIDE, OUTLINE_SIDE) { _, _ -> 255 }

    // 캐시가 프로세스 전역이라 테스트 사이에 판이 넘어간다
    @Before
    fun setUp() {
        ToppingBorderPlateCache.clear()
    }

    @Test
    fun sizesFarApart_bothStayRetrievable() {
        // Given 같은 토핑을 목록 크기와 캔버스 크기로 그려 넣은 판(1.25배 넘게 차이)
        val listPlate = plate(subjectLongSide = 264)
        val canvasPlate = plate(subjectLongSide = 432)
        put(listPlate)
        put(canvasPlate)

        // When 각 크기로 꺼낸다
        // Then 나중에 넣은 판이 먼저 넣은 판을 덮어쓰지 않는다
        assertSame(listPlate, get(subjectLongSide = 264))
        assertSame(canvasPlate, get(subjectLongSide = 432))
    }

    @Test
    fun sizeWithinReuseRatio_replacesOlderPlate() {
        // Given 1.25배 안으로 크기가 가까운 두 판
        val olderPlate = plate(subjectLongSide = 400)
        val newerPlate = plate(subjectLongSide = 440)
        put(olderPlate)
        put(newerPlate)

        // When 옛 판에만 맞는 크기로 꺼낸다(330/400 은 맞고 330/440 은 안 맞는다)
        // Then 옛 판은 이미 대체되어 없다
        assertNull(get(subjectLongSide = 330))
        assertSame(newerPlate, get(subjectLongSide = 400))
    }

    @Test
    fun morePlatesThanShelfHolds_dropsOldest() {
        // Given 서로 맞지 않는 크기의 판 넷(선반은 셋을 담는다)
        put(plate(subjectLongSide = 100))
        put(plate(subjectLongSide = 200))
        put(plate(subjectLongSide = 400))
        put(plate(subjectLongSide = 800))

        // Then 가장 먼저 넣은 판만 빠진다
        assertNull(get(subjectLongSide = 100))
        assertNotNull(get(subjectLongSide = 200))
        assertNotNull(get(subjectLongSide = 400))
        assertNotNull(get(subjectLongSide = 800))
    }

    @Test
    fun withoutSize_returnsMostRecentlyUsedPlate() {
        // Given 두 크기의 판
        val smallPlate = plate(subjectLongSide = 100)
        val largePlate = plate(subjectLongSide = 400)
        put(smallPlate)
        put(largePlate)

        // Then 크기 없이 꺼내면 마지막에 넣은 판이다
        assertSame(largePlate, get(subjectLongSide = null))

        // When 작은 판을 크기로 꺼내 쓴다
        get(subjectLongSide = 100)

        // Then 크기 없이 꺼내면 방금 쓴 판이다
        assertSame(smallPlate, get(subjectLongSide = null))
    }

    @Test
    fun noPlateFitsSize_returnsNull() {
        // Given 작은 판 하나
        put(plate(subjectLongSide = 100))

        // Then 네 배 큰 크기에는 꺼낼 판이 없다
        assertNull(get(subjectLongSide = 400))
    }

    @Test
    fun twoPlatesFitSize_returnsMoreRecentOne() {
        // Given 서로 대체되지 않지만(130/100 = 1.3) 크기 115 에는 둘 다 맞는 두 판
        put(plate(subjectLongSide = 100))
        val newerPlate = plate(subjectLongSide = 130)
        put(newerPlate)

        // Then 더 최근에 넣은 판이 나온다
        assertSame(newerPlate, get(subjectLongSide = 115))
    }

    @Test
    fun differentOutset_doesNotShareShelf() {
        // Given 굵기 4px 로 만든 판
        put(plate(subjectLongSide = 100), outsetPx = 4f)

        // Then 굵기 8px 로는 같은 크기라도 꺼내지지 않는다
        assertNull(get(subjectLongSide = 100, outsetPx = 8f))
    }

    private fun plate(subjectLongSide: Int) = ToppingBorderPlate(
        image = ImageBitmap(1, 1),
        padding = 0,
        subjectLongSide = subjectLongSide,
    )

    private fun put(
        plate: ToppingBorderPlate,
        outsetPx: Float = OUTSET_PX,
    ) = ToppingBorderPlateCache.put(outline, outsetPx, ASPECT_RATIO, plate)

    private fun get(
        subjectLongSide: Int?,
        outsetPx: Float = OUTSET_PX,
    ): ToppingBorderPlate? = ToppingBorderPlateCache.get(outline, outsetPx, ASPECT_RATIO, subjectLongSide)

    private companion object {
        const val OUTLINE_SIDE = 8
        const val OUTSET_PX = 12f
        const val ASPECT_RATIO = 1f
    }
}
