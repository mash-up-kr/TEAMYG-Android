package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 판 해상도. 40x40 그림에 한 칸이 1px 로 대응해 8px 테두리도 칸 단위로 해상된다 — 더 성기면
 * 반 칸 편향이 굵기만 한 오차가 된다.
 */
private const val FIELD_SIDE = 40

/** 왼쪽 절반만 불투명한 판. 좌우 비대칭이라 좌표 부호 실수가 드러난다. */
private fun leftHalfOutline(): ToppingOutline =
    ToppingOutline.of(width = FIELD_SIDE, height = FIELD_SIDE) { x, _ -> if (x < 20) 255 else 0 }

/** 왼쪽 끝 열(0번 열)의 가운데 절반만 불투명한 판 — 실루엣이 판 왼쪽 변에 닿아 있다. */
private fun leftEdgeColumnOutline(): ToppingOutline =
    ToppingOutline.of(width = FIELD_SIDE, height = FIELD_SIDE) { x, y -> if (x == 0 && y in 10..29) 255 else 0 }

/** 위 판을 90도 돌린 것과 같다 — 위쪽 밖 판정을 같은 방식으로 본다. */
private fun topEdgeRowOutline(): ToppingOutline =
    ToppingOutline.of(width = FIELD_SIDE, height = FIELD_SIDE) { x, y -> if (y == 0 && x in 10..29) 255 else 0 }

private fun target(
    rotationDegrees: Float = 0f,
    borderWidthPx: Float = 0f,
    outline: ToppingOutline? = leftHalfOutline(),
): ToppingHitTarget = ToppingHitTarget(
    centerXPx = 100f,
    centerYPx = 100f,
    imageWidthPx = 40f,
    imageHeightPx = 40f,
    rotationDegrees = rotationDegrees,
    borderWidthPx = borderWidthPx,
    outline = outline,
)

class ToppingHitTestTest {
    @Test
    fun containsPoint_opaqueSide_isHit() {
        // Given·When 왼쪽 절반(불투명)의 한가운데
        // Then
        assertTrue(target().containsPoint(90f, 100f))
    }

    @Test
    fun containsPoint_transparentSide_isMiss() {
        // Given·When 오른쪽 절반(투명)의 한가운데 — 그림 사각형 안이지만 안 눌려야 한다
        // Then
        assertFalse(target().containsPoint(110f, 100f))
    }

    @Test
    fun containsPoint_outsideImageRect_isMiss() {
        // Given·When 그림 사각형 밖
        // Then
        assertFalse(target().containsPoint(100f, 130f))
    }

    @Test
    fun containsPoint_rotated180_opaqueSideMovesToRight() {
        // Given 180도 돌리면 불투명한 왼쪽 절반이 오른쪽으로 온다
        val rotated = target(rotationDegrees = 180f)

        // Then
        assertTrue(rotated.containsPoint(110f, 100f))
        assertFalse(rotated.containsPoint(90f, 100f))
    }

    @Test
    fun containsPoint_rotated90_opaqueSideMovesToTop() {
        // Given 90도 시계방향 회전. containsPoint는 역변환(반시계방향)을 하므로
        // 원본의 불투명한 왼쪽이 위로 온다.
        // 계산: rotationDegrees=90 → radians=-π/2
        // cos(-π/2)=0, sin(-π/2)=-1
        // (100,90): dx=0, dy=-10 → localX=0-(-10)*(-1)=-10
        // → fieldX=(-10+20)-0.5=9.5 → 10번 칸 < 20 → 불투명
        // (100,110): dx=0, dy=10 → localX=0-10*(-1)=10
        // → fieldX=(10+20)-0.5=29.5 → 30번 칸 >= 20 → 투명
        val rotated = target(rotationDegrees = 90f)

        // Then 불투명한 쪽이 위로 이동했다
        assertTrue(rotated.containsPoint(100f, 90f))
        assertFalse(rotated.containsPoint(100f, 110f))
    }

    @Test
    fun containsPoint_withBorder_extendsBeyondSilhouette() {
        // Given 테두리 8px 인 토핑. 투명한 오른쪽이지만 불투명 경계에서 8px 안쪽이다
        val bordered = target(borderWidthPx = 8f)

        // Then 테두리가 있으면 히트, 없으면 미스다
        assertTrue(bordered.containsPoint(104f, 100f))
        assertFalse(target(borderWidthPx = 0f).containsPoint(104f, 100f))
    }

    @Test
    fun containsPoint_withBorder_farTransparentStillMiss() {
        // Given 테두리 두께보다 훨씬 멀리 떨어진 투명한 자리
        val bordered = target(borderWidthPx = 4f)

        // Then
        assertFalse(bordered.containsPoint(118f, 100f))
    }

    @Test
    fun containsPoint_nullOutline_fallsBackToRectangle() {
        // Given 거리판이 아직 없는 토핑
        val noOutline = target(outline = null)

        // Then 그림 사각형 안이면 투명한 자리여도 히트다 — 현행 판정과 같다
        assertTrue(noOutline.containsPoint(110f, 100f))
        assertFalse(noOutline.containsPoint(100f, 130f))
    }

    @Test
    fun containsPoint_emptyOutline_fallsBackToRectangle() {
        // Given 불투명 픽셀이 하나도 없는 판 — 부재로 봐야 영영 안 눌리는 일이 없다
        val empty = target(
            outline = ToppingOutline.of(FIELD_SIDE, FIELD_SIDE) { _, _ -> 0 },
        )

        // Then
        assertTrue(empty.containsPoint(110f, 100f))
    }

    @Test
    fun containsPoint_leftOfImageRect_isHitWhenTheBorderReachesThere() {
        // 옛 판정은 그림 사각형 밖을 무조건 투명으로 답해 여기서 미스였다. 새 판정은 판 밖 거리를
        // 재므로, 실루엣이 그림 왼쪽 변에 닿아 있으면 테두리를 그린 자리까지 눌린다.
        // 그리는 모양과 판정을 일치시키는 것이 이 라운드의 목적이라 이쪽이 맞다.
        // Given 테두리 15px 인 토핑. 그림 왼쪽 가장자리에서 4px 밖(fieldX=-4.5)을 짚는다
        val bordered = target(borderWidthPx = 15f, outline = leftEdgeColumnOutline())

        // Then 0번 열이 불투명하니 판 밖 거리가 4.5 로 15 안쪽이다
        assertTrue(bordered.containsPoint(76f, 100f))
    }

    @Test
    fun containsPoint_aboveImageRect_isHitWhenTheBorderReachesThere() {
        // Given 위쪽 대칭 사례 — 0번 행만 불투명하고 그림 위 가장자리 바로 밖을 짚는다
        val bordered = target(borderWidthPx = 15f, outline = topEdgeRowOutline())

        // Then
        assertTrue(bordered.containsPoint(100f, 76f))
    }

    @Test
    fun containsPoint_borderWidth_extendsTheHitAreaByThatDistance() {
        // Given 가운데 한 칸만 불투명한 40x40 실루엣을 40x40 픽셀로 그린다
        val outline = ToppingOutline.of(width = 40, height = 40) { x, y ->
            if (x == 20 && y == 20) 255 else 0
        }
        val target = ToppingHitTarget(
            centerXPx = 20f,
            centerYPx = 20f,
            imageWidthPx = 40f,
            imageHeightPx = 40f,
            rotationDegrees = 0f,
            borderWidthPx = 5f,
            outline = outline,
        )

        // Then 실루엣에서 5 떨어진 자리는 눌리고 7 떨어진 자리는 안 눌린다
        assertTrue(target.containsPoint(15.5f, 20.5f))
        assertFalse(target.containsPoint(13.5f, 20.5f))
    }

    @Test
    fun containsPoint_withoutBorder_onlyTheSilhouetteIsHit() {
        val outline = ToppingOutline.of(width = 40, height = 40) { x, y ->
            if (x == 20 && y == 20) 255 else 0
        }
        val target = ToppingHitTarget(
            centerXPx = 20f,
            centerYPx = 20f,
            imageWidthPx = 40f,
            imageHeightPx = 40f,
            rotationDegrees = 0f,
            borderWidthPx = 0f,
            outline = outline,
        )

        // Then 안 그린 테두리만큼 판정이 넓어지면 안 된다
        assertTrue(target.containsPoint(20.5f, 20.5f))
        assertFalse(target.containsPoint(17.5f, 20.5f))
    }

    @Test
    fun containsPoint_fallsBackToTheRectangleWhenNothingIsOpaque() {
        val outline = ToppingOutline.of(width = 40, height = 40) { _, _ -> 0 }
        val target = ToppingHitTarget(
            centerXPx = 20f,
            centerYPx = 20f,
            imageWidthPx = 40f,
            imageHeightPx = 40f,
            rotationDegrees = 0f,
            borderWidthPx = 0f,
            outline = outline,
        )

        // Then 실루엣을 못 읽으면 사각형으로 받는다 — 아무 데도 안 눌리는 것보다 낫다
        assertTrue(target.containsPoint(2f, 2f))
    }
}

/** 왼쪽 절반만 불투명한 40x40 그림. 판 한 칸이 1px 이라 자리마다 알파가 갈린다. */
private fun targetCenteredAt(centerXPx: Float): ToppingHitTarget = ToppingHitTarget(
    centerXPx = centerXPx,
    centerYPx = 100f,
    imageWidthPx = 40f,
    imageHeightPx = 40f,
    rotationDegrees = 0f,
    borderWidthPx = 0f,
    outline = leftHalfOutline(),
)

class PickToppingHitTest {
    @Test
    fun pickToppingHit_topOpaque_picksTop() {
        // Given 같은 자리에 겹친 둘. 목록은 아래에서 위 순서다
        val entries = listOf(
            "bottom" to targetCenteredAt(100f),
            "top" to targetCenteredAt(100f),
        )

        // When 둘 다 불투명한 자리를 누른다
        // Then 위 것이 잡힌다
        assertEquals("top", pickToppingHit(entries, 90f, 100f))
    }

    @Test
    fun pickToppingHit_topTransparent_fallsThroughToBottom() {
        // Given 위 것의 투명한 오른쪽 절반에, 아래 것의 불투명한 왼쪽 절반이 깔려 있다
        val entries = listOf(
            "bottom" to targetCenteredAt(120f),
            "top" to targetCenteredAt(100f),
        )

        // Then 위를 통과해 아래가 잡힌다
        assertEquals("bottom", pickToppingHit(entries, 110f, 100f))
    }

    @Test
    fun pickToppingHit_bothTransparent_picksNothing() {
        // Given 둘 다 그림 사각형 안이지만 그 자리가 투명하다
        val entries = listOf(
            "bottom" to targetCenteredAt(100f),
            "top" to targetCenteredAt(100f),
        )

        // Then
        assertNull(pickToppingHit(entries, 110f, 100f))
    }
}
