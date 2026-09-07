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

    @Test
    fun buildBorderAlpha_scalesTheOutsetByTheSubjectToFieldRatio() {
        // Given 왼쪽 열 전체가 불투명한 8x8 판을 subjectWidth 4 에 앉힌다 — 배율 2.0(필드픽셀/목표픽셀).
        // 왼쪽 열 전체가 씨앗이라 어느 행에서 재도 거리는 그 칸의 x 좌표 그대로다
        val outline = outlineOf(
            "#.......",
            "#.......",
            "#.......",
            "#.......",
            "#.......",
            "#.......",
            "#.......",
            "#.......",
        )
        val target = ToppingBorderTarget(
            width = 4,
            height = 4,
            subjectLeft = 0,
            subjectTop = 0,
            subjectWidth = 4,
            subjectHeight = 4,
        )

        // When 목표 좌표계로 1px 만큼 두른다 — 필드 좌표로는 1 * 2.0 = 2.0
        val alpha = outline.buildBorderAlpha(target, outsetPx = 1f)

        // Then 목표 x=0..3 이 필드 x=0.5,2.5,4.5,6.5 로 옮겨진다(배율 2, 반 픽셀 오프셋).
        // 거리는 필드 x 그대로이므로:
        //   x=0: 거리 0.5, coverage=(2.0-0.5)*0.5+0.5=1.25 → 1.0 로 잘려 알파 255
        //   x=1: 거리 2.5, coverage=(2.0-2.5)*0.5+0.5=0.25 → 알파 round(0.25*255)=64
        //   x=2: 거리 4.5 > outermostEdge(2.0+0.5*2=3.0) → 칸 밖, 알파 0
        //   x=3: 거리 6.5 > 3.0 → 알파 0
        // 배율을 뒤집어 0.5 를 썼다면 x=1 이 이미 outermostEdge 밖으로 나가 0 이 되므로
        // 이 네 값이 배율이 맞게 곱해졌는지를 그대로 가른다
        assertNotNull(alpha)
        assertEquals(255, alpha.alphaAt(0), "필드 거리 0.5 인 x=0 이 완전히 칠해지지 않았다")
        assertEquals(64, alpha.alphaAt(1), "필드 거리 2.5 인 x=1 의 부분 커버리지가 어긋났다")
        assertEquals(0, alpha.alphaAt(2), "필드 거리 4.5 인 x=2 는 outermostEdge 밖이라 비어야 한다")
        assertEquals(0, alpha.alphaAt(3), "필드 거리 6.5 인 x=3 은 outermostEdge 밖이라 비어야 한다")
    }

    @Test
    fun buildBorderPixels_isNullWhenEveryBandHasNoOutset() {
        // Given 씨앗은 있지만 겹이 굵기 0 하나뿐이다 — 두를 폭이 없다
        val outline = outlineOf(
            "...",
            ".#.",
            "...",
        )

        val pixels = outline.buildBorderPixels(
            target = wholeTarget(3, 3),
            bands = listOf(ToppingBorderBand(outsetPx = 0f, colorArgb = 0xFFFF0000.toInt())),
        )

        // Then 굵기 0 을 요청했으니 반투명 테두리가 아니라 null 이어야 한다
        assertNull(pixels)
    }
}
