package com.teamyg.parfait.feature.segmentation.impl.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 테두리가 번져 나갈 자리로 사방에 남겨 두는 여백 */
private const val PADDING = 20

class ToppingBorderPreviewLayoutTest {
    @Test
    fun layout_fitsTheSubjectInsideThePadding() {
        // Given 400x400 뷰에 200x100 알맹이를 그린다. 여백을 뺀 360x360 안에 들어가야 한다
        val layout = toppingBorderPreviewLayoutOrNull(
            viewWidth = 400,
            viewHeight = 400,
            bitmapWidth = 200,
            bitmapHeight = 100,
            paddingPx = PADDING,
        )

        // Then 배율은 여백을 뺀 크기 기준이고(360/200), 판은 알맹이에 사방 여백을 더한 크기다
        assertEquals(1.8f, layout?.scale)
        assertEquals(360, layout?.subjectWidth)
        assertEquals(180, layout?.subjectHeight)
        assertEquals(400, layout?.canvasWidth)
        assertEquals(220, layout?.canvasHeight)
    }

    @Test
    fun layout_centersTheCanvasInTheView() {
        val layout = toppingBorderPreviewLayoutOrNull(
            viewWidth = 400,
            viewHeight = 400,
            bitmapWidth = 200,
            bitmapHeight = 100,
            paddingPx = PADDING,
        )

        // Then 남는 자리는 위아래로 똑같이 나뉜다. 알맹이가 아니라 여백까지 두른 판이 가운데다
        assertEquals(0, layout?.offsetX)
        assertEquals(90, layout?.offsetY)
    }

    @Test
    fun layout_isNullWhenThePaddingLeavesNoRoom() {
        // Given 여백 둘이 뷰보다 넓다 — 알맹이를 놓을 자리가 없다
        val layout = toppingBorderPreviewLayoutOrNull(
            viewWidth = 30,
            viewHeight = 30,
            bitmapWidth = 200,
            bitmapHeight = 100,
            paddingPx = PADDING,
        )

        // Then 배율을 0 이하로 만들어 빈 비트맵을 만드는 대신 그리지 않는다
        assertNull(layout)
    }

    @Test
    fun layout_isNullWhenTheViewHasNoSize() {
        // Given 아직 측정되지 않은 뷰
        val layout = toppingBorderPreviewLayoutOrNull(
            viewWidth = 0,
            viewHeight = 0,
            bitmapWidth = 200,
            bitmapHeight = 100,
            paddingPx = PADDING,
        )

        assertNull(layout)
    }
}
