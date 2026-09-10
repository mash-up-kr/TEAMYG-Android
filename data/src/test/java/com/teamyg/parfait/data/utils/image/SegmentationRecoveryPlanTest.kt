package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SegmentationRecoveryPlanTest {
    private val tenfold = RecoveryTransform(scaleX = 10f, scaleY = 10f, offsetX = 0, offsetY = 0)

    @Test
    fun resolveTargetSize_shortSideBelowTheFloor_upscalesKeepingTheRatio() {
        assertEquals(ScaledSize(width = 683, height = 512), resolveTargetSize(width = 400, height = 300))
    }

    @Test
    fun resolveTargetSize_shortSideExactlyTheFloor_doesNotChange() {
        assertEquals(ScaledSize(width = 683, height = 512), resolveTargetSize(width = 683, height = 512))
    }

    @Test
    fun resolveTargetSize_longSideAboveTheCeiling_downscales() {
        assertEquals(ScaledSize(width = 2048, height = 1536), resolveTargetSize(width = 4032, height = 3024))
    }

    @Test
    fun resolveTargetSize_floorAndCeilingConflict_theCeilingWins() {
        // Given 하한을 맞추면 긴 변이 상한을 넘는 극단 종횡비
        val target = resolveTargetSize(width = 5000, height = 400)

        // Then 상한이 이겨 짧은 변은 하한에 못 미친다
        assertEquals(ScaledSize(width = 2048, height = 164), target)
        assertTrue(target.height < DETECTION_MIN_SHORT_SIDE)
    }

    @Test
    fun normalizeStage_targetEqualsSourceAndNoContrast_isNull() {
        assertNull(normalizeStage(width = 1920, height = 1080, applyContrast = false))
    }

    @Test
    fun normalizeStage_targetEqualsSourceButContrastApplies_isNotNull() {
        assertNotNull(normalizeStage(width = 1920, height = 1080, applyContrast = true))
    }

    @Test
    fun normalizeStage_hasNoCropAndNoOffset() {
        val stage = requireNotNull(normalizeStage(width = 4032, height = 3024, applyContrast = true))

        assertNull(stage.cropRect)
        assertEquals(0, stage.transform.offsetX)
        assertEquals(0, stage.transform.offsetY)
    }

    @Test
    fun hintBounds_onePixelAboveTheThreshold_wrapsItExclusively() {
        val alpha = ByteArray(9)
        alpha[4] = 200.toByte()

        assertEquals(DetectionBounds(1, 1, 2, 2), hintBounds(alpha, width = 3, height = 3, threshold = 127))
    }

    @Test
    fun hintBounds_scatteredPixels_wrapsThemAll() {
        // Given 대각선 양 끝의 두 점 — 최소·최대를 갱신하지 않고 마지막 값을 대입하면 틀린다
        val alpha = ByteArray(9)
        alpha[0] = 200.toByte()
        alpha[8] = 200.toByte()

        assertEquals(DetectionBounds(0, 0, 3, 3), hintBounds(alpha, width = 3, height = 3, threshold = 127))
    }

    @Test
    fun hintBounds_nothingAboveTheThreshold_isNull() {
        assertNull(hintBounds(ByteArray(9), width = 3, height = 3, threshold = 127))
    }

    @Test
    fun hintBounds_valueEqualToTheThreshold_isExcluded() {
        assertNull(hintBounds(ByteArray(4) { 127.toByte() }, width = 2, height = 2, threshold = 127))
    }

    @Test
    fun focusCrop_smallHint_addsTheMarginOnEverySide() {
        val crop = focusCrop(1000, 1000, hint = DetectionBounds(40, 40, 60, 60), hintTransform = tenfold)

        // 원본 좌표 400..600, 크기 200 의 20% 인 40 이 각 변에 붙는다
        assertEquals(SegmentationBounds(left = 360, top = 360, right = 640, bottom = 640), crop)
    }

    @Test
    fun focusCrop_hintTouchesTheEdge_clampsToTheOrigin() {
        val crop = focusCrop(1000, 1000, hint = DetectionBounds(0, 0, 10, 10), hintTransform = tenfold)

        assertEquals(SegmentationBounds(left = 0, top = 0, right = 120, bottom = 120), crop)
    }

    @Test
    fun focusCrop_noHintOnALandscapeOrigin_isACenteredSquareOfTheShortSide() {
        // Given 가로가 긴 원본 — 축마다 70% 로 자르면 2800x2100 이 되어 틀린다
        val crop = focusCrop(4000, 3000, hint = null, hintTransform = null)

        assertEquals(SegmentationBounds(left = 950, top = 450, right = 3050, bottom = 2550), crop)
    }

    @Test
    fun focusStage_cropIsExactlySeventyPercent_isNull() {
        // Given 넓이가 원본의 정확히 70% — 비교를 > 로 바꾸면 통과해 버리는 경계
        assertNull(focusStage(10, 10, crop = SegmentationBounds(0, 0, 7, 10), applyContrast = true))
    }

    @Test
    fun focusStage_cropIsSmaller_carriesTheCropAsTheOffset() {
        val crop = SegmentationBounds(left = 360, top = 360, right = 640, bottom = 640)

        val stage = requireNotNull(focusStage(1000, 1000, crop, applyContrast = true))

        assertEquals(crop, stage.cropRect)
        assertEquals(ScaledSize(512, 512), stage.targetSize)
        assertEquals(360, stage.transform.offsetX)
        assertEquals(360, stage.transform.offsetY)
    }

    @Test
    fun cropAreaPercent_halfTheArea_isFifty() {
        assertEquals(50, cropAreaPercent(SegmentationBounds(0, 0, 50, 100), width = 100, height = 100))
    }

    @Test
    fun recoveryTransform_differentScalesPerAxis_mapsEachAxisWithItsOwnScale() {
        // Given 축마다 배율이 다른 변환 — 같은 배율 픽스처만 있으면 scaleY 대신 scaleX 를 써도 통과한다
        val transform = RecoveryTransform(scaleX = 2f, scaleY = 3f, offsetX = 10, offsetY = 20)

        assertEquals(SegmentationBounds(12, 23, 20, 35), transform.toOrigin(DetectionBounds(1, 1, 5, 5)))
    }

    @Test
    fun recoveryTransform_roundTrip_returnsWithinOnePixel() {
        // Given 상한에 걸려 축소되고 비율도 딱 안 떨어지는 크롭 — 배율이 1 이면 반올림을 안 탄다
        val crop = SegmentationBounds(left = 137, top = 251, right = 4137, bottom = 2918)
        val target = resolveTargetSize(crop.width, crop.height)
        val transform = RecoveryTransform(
            scaleX = crop.width.toFloat() / target.width,
            scaleY = crop.height.toFloat() / target.height,
            offsetX = crop.left,
            offsetY = crop.top,
        )

        val origin = transform.toOrigin(DetectionBounds(0, 0, target.width, target.height))

        assertTrue(abs(origin.left - crop.left) <= ROUND_TRIP_TOLERANCE_PX)
        assertTrue(abs(origin.top - crop.top) <= ROUND_TRIP_TOLERANCE_PX)
        assertTrue(abs(origin.right - crop.right) <= ROUND_TRIP_TOLERANCE_PX)
        assertTrue(abs(origin.bottom - crop.bottom) <= ROUND_TRIP_TOLERANCE_PX)
    }

    @Test
    fun projectRegion_mappedRectCrossesTheCrop_clipsToTheIntersectionAndKeepsTheMappedRect() {
        val projection = DetectionProjection(
            transform = RecoveryTransform(scaleX = 2f, scaleY = 2f, offsetX = 100, offsetY = 100),
            clip = SegmentationBounds(150, 150, 400, 400),
        )

        val projected = requireNotNull(projectRegion(DetectionBounds(0, 0, 100, 100), projection, 1000, 1000))

        // 재표본은 사상 사각형 크기로 하므로 잘리기 전 사각형도 들고 나와야 한다
        assertEquals(SegmentationBounds(100, 100, 300, 300), projected.mapped)
        assertEquals(SegmentationBounds(150, 150, 300, 300), projected.clipped)
    }

    @Test
    fun projectRegion_noOverlapWithTheCrop_isNull() {
        val projection = DetectionProjection(
            transform = RecoveryTransform(scaleX = 1f, scaleY = 1f, offsetX = 0, offsetY = 0),
            clip = SegmentationBounds(500, 500, 600, 600),
        )

        assertNull(projectRegion(DetectionBounds(0, 0, 10, 10), projection, 1000, 1000))
    }

    @Test
    fun offsetBy_movesEveryEdge() {
        assertEquals(SegmentationBounds(11, 22, 13, 24), SegmentationBounds(1, 2, 3, 4).offsetBy(dx = 10, dy = 20))
    }

    @Test
    fun isInsideCanvas_exactFit_isTrue() {
        assertTrue(isInsideCanvas(SegmentationBounds(0, 0, 100, 50), canvasWidth = 100, canvasHeight = 50))
    }

    @Test
    fun isInsideCanvas_anyEdgeOutside_isFalse() {
        assertFalse(isInsideCanvas(SegmentationBounds(-1, 0, 100, 50), canvasWidth = 100, canvasHeight = 50))
        assertFalse(isInsideCanvas(SegmentationBounds(0, -1, 100, 50), canvasWidth = 100, canvasHeight = 50))
        assertFalse(isInsideCanvas(SegmentationBounds(0, 0, 101, 50), canvasWidth = 100, canvasHeight = 50))
        assertFalse(isInsideCanvas(SegmentationBounds(0, 0, 100, 51), canvasWidth = 100, canvasHeight = 50))
    }
}
