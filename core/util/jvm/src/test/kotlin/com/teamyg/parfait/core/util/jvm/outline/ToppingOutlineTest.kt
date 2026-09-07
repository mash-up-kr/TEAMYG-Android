package com.teamyg.parfait.core.util.jvm.outline

import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val OPAQUE = 255
private const val TRANSPARENT = 0
private const val TOLERANCE = 0.2f

/** 실루엣을 그림으로 준다. `#` 이 불투명한 자리다 */
private fun outlineOf(vararg rows: String): ToppingOutline =
    ToppingOutline.of(width = rows.first().length, height = rows.size) { x, y ->
        if (rows[y][x] == '#') OPAQUE else TRANSPARENT
    }

/** 알맹이가 판을 그대로 채우는 목표. 여백을 따지지 않는 테스트가 쓴다 */
private fun wholeTarget(
    width: Int,
    height: Int,
): ToppingBorderTarget = ToppingBorderTarget(
    width = width,
    height = height,
    subjectLeft = 0,
    subjectTop = 0,
    subjectWidth = width,
    subjectHeight = height,
)

/** ByteArray 는 부호가 있어 255 가 -1 로 담긴다. 눈으로 읽을 0~255 로 되돌린다 */
private fun ByteArray.alphaAt(index: Int): Int = this[index].toInt() and 0xFF

class ToppingOutlineTest {
    @Test
    fun buildBorderAlpha_fillsEveryCellWithinTheOutset() {
        // Given 한가운데 한 칸만 불투명한 15x15 판
        val size = 15
        val center = 7
        val outline = ToppingOutline.of(size, size) { x, y ->
            if (x == center && y == center) OPAQUE else TRANSPARENT
        }

        // When 거리 5 까지 두른다
        val outset = 5f
        val alpha = outline.buildBorderAlpha(wholeTarget(size, size), outsetPx = outset)

        // Then 거리 5 이하인 칸이 하나도 빠짐없이 칠해진다.
        // 여덟 방향 스탬프는 여기서 여덟 갈래 꽃잎을 만들어 스탬프 사이가 빈다 — 이 단언이
        // 이번 작업의 목적을 코드로 고정한다
        assertNotNull(alpha)
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (hypot((x - center).toFloat(), (y - center).toFloat()) > outset) continue
                assertTrue(alpha.alphaAt(y * size + x) > 0, "거리 $outset 안쪽 ($x,$y) 이 비었다")
            }
        }

        // 그리고 스탬프가 절대 못 닿는 자리 하나를 따로 못박는다 — 씨앗에서 (3,-3) 은
        // 여덟 방향 어느 이동으로도 나오지 않는다
        assertTrue(alpha.alphaAt(4 * size + 10) > 0)
    }

    @Test
    fun buildBorderAlpha_edgeSitsAtTheOutsetDistance() {
        // Given 한가운데 한 칸만 불투명한 7x7 판
        val outline = outlineOf(
            ".......",
            ".......",
            ".......",
            "...#...",
            ".......",
            ".......",
            ".......",
        )

        // When 거리 2 까지 칠한다
        val alpha = outline.buildBorderAlpha(wholeTarget(7, 7), outsetPx = 2f)

        // Then 거리 2 인 자리는 남고 거리 3 인 자리는 비어 있다
        assertNotNull(alpha)
        assertTrue(alpha.alphaAt(3 * 7 + 1) > 0, "거리 2 인 자리가 비었다")
        assertEquals(0, alpha.alphaAt(3 * 7 + 0), "거리 3 인 자리가 칠해졌다")
    }

    @Test
    fun buildBorderAlpha_keepsTheSubjectInsideThePadding() {
        // Given 4x4 실루엣을 사방 3 씩 비운 10x10 판에 앉힌다
        val outline = outlineOf(
            "####",
            "####",
            "####",
            "####",
        )
        val target = ToppingBorderTarget(
            width = 10,
            height = 10,
            subjectLeft = 3,
            subjectTop = 3,
            subjectWidth = 4,
            subjectHeight = 4,
        )

        // When 거리 2 까지 두른다
        val alpha = outline.buildBorderAlpha(target, outsetPx = 2f)

        // Then 알맹이 자리는 채워지고, 여백 바깥 끝은 비어 있다.
        // 알맹이 자리를 안 받으면 실루엣이 판 전체로 늘어나 이 칸까지 칠해진다
        assertNotNull(alpha)
        assertTrue(alpha.alphaAt(5 * 10 + 5) > 0, "알맹이 한가운데가 비었다")
        assertEquals(0, alpha.alphaAt(5 * 10 + 0), "여백 바깥 끝이 칠해졌다")
    }

    @Test
    fun buildBorderAlpha_isNullWhenNothingIsOpaque() {
        // Given 전부 투명한 판 — 실루엣이 없으면 두를 대상도 없다
        val outline = outlineOf(
            "....",
            "....",
        )

        assertFalse(outline.hasAnySeed)
        assertNull(outline.buildBorderAlpha(wholeTarget(4, 2), outsetPx = 1f))
    }

    @Test
    fun distanceAt_interpolatesBetweenCells() {
        // Given 왼쪽 한 줄만 불투명하다 — 거리가 x 그대로다
        val outline = outlineOf(
            "#....",
            "#....",
        )

        // Then 칸 사이를 읽으면 두 칸 값의 중간이 나온다
        assertEquals(2.5f, outline.distanceAt(2.5f, 0f), TOLERANCE)
    }

    @Test
    fun distanceAt_outsideTheField_addsTheOverflowInsteadOfClamping() {
        // Given 왼쪽 끝 한 칸만 불투명하다
        val outline = outlineOf("#..")

        // Then 판 왼쪽 밖 2 칸은 거리 0 이 아니라 2 다.
        // 가장자리 값으로 고정하면 실루엣이 판 변에 닿은 토핑에서 여백이 통째로 칠해진다
        assertEquals(2f, outline.distanceAt(-2f, 0f), TOLERANCE)
    }

    @Test
    fun isOpaqueAt_readsTheNearestCellWithoutInterpolating() {
        // Given 불투명한 칸과 투명한 칸이 붙어 있다
        val outline = outlineOf("#.")

        // Then 경계 근처는 보간값이 아니라 가장 가까운 칸의 답을 준다
        assertTrue(outline.isOpaqueAt(0.4f, 0f))
        assertFalse(outline.isOpaqueAt(0.6f, 0f))
    }

    @Test
    fun distanceAt_quantizationErrorStaysWithinAnEighthOfAPixel() {
        // Given 왼쪽 위 한 칸만 불투명해 대각 거리가 1/8 의 배수가 아닌 판
        val outline = outlineOf(
            "#..",
            "...",
        )

        // Then √5 = 2.2360…은 눈금에 딱 안 맞지만 오차가 1/16 을 넘지 않는다
        assertEquals(sqrt(5f), outline.distanceAt(2f, 1f), 1f / 16f)
    }

    @Test
    fun buildBorderPixels_paintsEachBandWithItsOwnColor() {
        // Given 한가운데 한 칸만 불투명한 9x9 판에 색이 다른 두 겹을 두른다
        val outline = ToppingOutline.of(9, 9) { x, y ->
            if (x == 4 && y == 4) OPAQUE else TRANSPARENT
        }
        val red = 0xFFFF0000.toInt()
        val blue = 0xFF0000FF.toInt()

        // When 안쪽 겹이 거리 2 까지, 바깥 겹이 거리 4 까지다
        val pixels = outline.buildBorderPixels(
            target = wholeTarget(9, 9),
            bands = listOf(
                ToppingBorderBand(outsetPx = 2f, colorArgb = red),
                ToppingBorderBand(outsetPx = 4f, colorArgb = blue),
            ),
        )

        // Then 실루엣 자리는 안쪽 겹 색, 바깥 겹 한복판은 바깥 겹 색이다
        assertNotNull(pixels)
        assertEquals(red, pixels[4 * 9 + 4])
        assertEquals(blue, pixels[4 * 9 + 1])

        // 겹 경계(거리 2)는 두 색을 반씩 섞은 자리라 어느 쪽 원색도 아니다
        val boundary = pixels[4 * 9 + 2]
        assertNotEquals(red, boundary)
        assertNotEquals(blue, boundary)
    }
}
