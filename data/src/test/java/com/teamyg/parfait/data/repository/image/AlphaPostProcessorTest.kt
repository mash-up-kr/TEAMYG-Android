package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 각 값이 알파 하나. 행 구분은 호출부가 width 로 준다 */
private fun alphaBytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

private fun ByteArray.asInts() = IntArray(size) { this[it].toInt() and 0xFF }

/** 왼쪽 위가 파인 마스크. bbox 안에 투명 픽셀이 남아야 정련이 일할 거리가 있다 */
private fun notchedMask() = ByteArray(64) { index ->
    val x = index % 8
    val y = index / 8
    if (x >= 3 && !(y < 2 && x < 6)) 255.toByte() else 0
}

/** 원본 좌표 x >= 4 가 흰 안내자 */
private val splitGuidance = GuidanceProvider { bounds ->
    IntArray(bounds.width * bounds.height) { index ->
        if ((index % bounds.width) + bounds.left >= 4) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
    }
}

class AlphaPostProcessorTest {
    @Test
    fun applyKeepMask_maskIsFalse_clearsThatBlock() {
        // Given — 2×2 원본, 배율 2 라 축소판은 1픽셀이다
        val alpha = alphaBytes(255, 255, 255, 255)
        val keep = booleanArrayOf(false)

        // When
        val changed = applyKeepMask(alpha, width = 2, height = 2, keep = keep, maskWidth = 1, factor = 2)

        // Then
        assertContentEquals(intArrayOf(0, 0, 0, 0), alpha.asInts())
        assertEquals(true, changed)
    }

    @Test
    fun applyKeepMask_maskIsTrue_leavesTheOriginalAlphaUntouched() {
        // Given — 이진화 결과로 알파를 덮어쓰지 않는 것이 이 설계의 요점이다
        val alpha = alphaBytes(10, 200, 255, 0)
        val keep = booleanArrayOf(true)

        // When
        val changed = applyKeepMask(alpha, width = 2, height = 2, keep = keep, maskWidth = 1, factor = 2)

        // Then
        assertContentEquals(intArrayOf(10, 200, 255, 0), alpha.asInts())
        assertEquals(false, changed)
    }

    @Test
    fun applyKeepMask_alphaAlreadyZero_doesNotReportChanged() {
        // Given — keep 이 거짓이어도 이미 0인 자리는 실제로 바뀌는 게 없다. changed 는 실제 변화만 세야
        // 3단계의 원판 재사용 분기가 살아남는다
        val alpha = alphaBytes(0, 0, 0, 0)
        val keep = booleanArrayOf(false)

        // When
        val changed = applyKeepMask(alpha, width = 2, height = 2, keep = keep, maskWidth = 1, factor = 2)

        // Then
        assertContentEquals(intArrayOf(0, 0, 0, 0), alpha.asInts())
        assertEquals(false, changed)
    }

    @Test
    fun measureAlpha_someOpaquePixels_returnsTightBoundsAndCoverage() {
        // Given — 4×3 에서 가운데 두 픽셀만 남았다
        val alpha = alphaBytes(
            0, 0, 0, 0,
            0, 100, 255, 0,
            0, 0, 0, 0,
        )

        // When
        val measured = measureAlpha(alpha, width = 4, height = 3)

        // Then — right·bottom 은 마지막 픽셀을 포함하도록 exclusive 다
        assertEquals(SegmentationBounds(left = 1, top = 1, right = 3, bottom = 2), measured?.bounds)
        assertEquals(355L, measured?.alphaSum)
        assertEquals(1, measured?.partialAlphaPixels)
    }

    @Test
    fun measureAlpha_everythingIsTransparent_returnsNull() {
        // Given
        val alpha = alphaBytes(0, 0, 0, 0)

        // When
        val measured = measureAlpha(alpha, width = 2, height = 2)

        // Then
        assertNull(measured)
    }

    @Test
    fun measureAlpha_alphaAbove127_isNotMisreadAsNegative() {
        // Given — 부호 처리를 빠뜨리면 합이 음수가 되고 bounds 도 안 잡힌다
        val alpha = alphaBytes(200, 255)

        // When
        val measured = measureAlpha(alpha, width = 2, height = 1)

        // Then
        assertEquals(455L, measured?.alphaSum)
        assertEquals(SegmentationBounds(left = 0, top = 0, right = 2, bottom = 1), measured?.bounds)
    }

    @Test
    fun erodeEdge_ramp_shiftsItInwardByOnePixelWithoutMakingAStep() {
        // Given — 한 행짜리 램프. 제자리에서 돌리면 왼쪽부터 연쇄로 전멸한다
        val alpha = alphaBytes(0, 64, 128, 191, 255, 255)

        // When
        val changed = erodeEdge(alpha, width = 6, height = 1)

        // Then
        assertEquals(true, changed)
        assertContentEquals(intArrayOf(0, 0, 64, 128, 191, 255), alpha.asInts())
    }

    @Test
    fun erodeEdge_verticalRamp_shiftsItInwardByOnePixelWithoutMakingAStep() {
        // Given — 테스트 1의 전치판. previousRow 스냅샷이 깨지면 위에서부터 연쇄로 전멸해
        // [0, 0, 0, 0, 0, 255] 가 나온다. up/down 이 최솟값을 결정하는 경로를 실행하는 유일한 테스트다
        val alpha = alphaBytes(0, 64, 128, 191, 255, 255)

        // When
        val changed = erodeEdge(alpha, width = 1, height = 6)

        // Then
        assertEquals(true, changed)
        assertContentEquals(intArrayOf(0, 0, 64, 128, 191, 255), alpha.asInts())
    }

    @Test
    fun erodeEdge_hardMatte_stillLosesOneLayer() {
        // Given — 알파가 0 아니면 255 뿐이다. "1~254 만 대상"이면 아무 일도 안 일어난다
        val alpha = alphaBytes(0, 255, 255, 255, 255, 0)

        // When
        erodeEdge(alpha, width = 6, height = 1)

        // Then
        assertContentEquals(intArrayOf(0, 0, 255, 255, 0, 0), alpha.asInts())
    }

    @Test
    fun erodeEdge_oneVerticalOpaqueLine_isProtectedByTheRidgeRule() {
        // Given — 폭 1 불투명 선. 좌우가 둘 다 0 이라 능선으로 보호한다
        val alpha = alphaBytes(
            0, 255, 0,
            0, 255, 0,
            0, 255, 0,
        )

        // When
        val changed = erodeEdge(alpha, width = 3, height = 3)

        // Then
        assertEquals(false, changed)
        assertContentEquals(intArrayOf(0, 255, 0, 0, 255, 0, 0, 255, 0), alpha.asInts())
    }

    @Test
    fun erodeEdge_oneVerticalPartialAlphaLine_isProtectedToo() {
        // Given — 값이 낮아도 능선이면 안 깎는다
        val alpha = alphaBytes(
            0, 100, 0,
            0, 100, 0,
            0, 100, 0,
        )

        // When
        erodeEdge(alpha, width = 3, height = 3)

        // Then
        assertContentEquals(intArrayOf(0, 100, 0, 0, 100, 0, 0, 100, 0), alpha.asInts())
    }

    @Test
    fun erodeEdge_twoPixelWideBar_disappears() {
        // Given — 양쪽에서 한 겹씩 깎이므로 사라진다. 1픽셀 침식에 내재한 한계다
        val alpha = alphaBytes(
            0, 255, 255, 0,
            0, 255, 255, 0,
            0, 255, 255, 0,
        )

        // When
        erodeEdge(alpha, width = 4, height = 3)

        // Then
        assertEquals(0, alpha.asInts().sum())
    }

    @Test
    fun erodeEdge_subjectTouchingTheFrame_keepsTheEdgeRow() {
        // Given — 판 전체가 불투명하다. 이미지 밖을 투명으로 치면 테두리가 깎인다
        // 2x2
        val alpha = alphaBytes(
            255,
            255,
            255,
            255,
        )

        // When
        val changed = erodeEdge(alpha, width = 2, height = 2)

        // Then
        assertEquals(false, changed)
        assertContentEquals(intArrayOf(255, 255, 255, 255), alpha.asInts())
    }

    @Test
    fun erodeEdge_descendingVerticalRamp_downDeterminesTheMinimum() {
        // Given — 내림 램프. 기존 세로 램프 테스트는 오름차순이라 up 이 항상 이긴다 — down 이 최솟값을
        // 결정하는 배치는 이 테스트가 유일하다
        val alpha = alphaBytes(255, 255, 191, 128, 64, 0)

        // When
        val changed = erodeEdge(alpha, width = 1, height = 6)

        // Then
        assertEquals(true, changed)
        assertContentEquals(intArrayOf(255, 191, 128, 64, 0, 0), alpha.asInts())
    }

    @Test
    fun erodeEdge_oneHorizontalOpaqueLine_isProtectedByTheUpDownRidgeRule() {
        // Given — 폭 1 가로 불투명 선. 좌우는 0 이 아니라 left/right 능선 분기에서 안 걸린다.
        // up·down 이 둘 다 0인 경우만 이 배치가 실행한다
        val alpha = alphaBytes(
            0, 0, 0,
            255, 255, 255,
            0, 0, 0,
        )

        // When
        val changed = erodeEdge(alpha, width = 3, height = 3)

        // Then
        assertEquals(false, changed)
        assertContentEquals(intArrayOf(0, 0, 0, 255, 255, 255, 0, 0, 0), alpha.asInts())
    }

    @Test
    fun postProcessAlpha_speckAwayFromTheBlob_isRemovedAndBoundsTightenToTheBlob() {
        // Given — 배율 1 층위. 8×8 에 4×4 덩어리와 떨어진 한 점
        val alpha = ByteArray(64)
        for (y in 0 until 4) for (x in 0 until 4) alpha[y * 8 + x] = 255.toByte()
        alpha[7 * 8 + 7] = 255.toByte()

        // When
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = AlphaPostProcessOptions(downscaleFactor = 1, areaOpeningMinPixels = 4, erodeEdge = false),
        )

        // Then
        assertEquals(SegmentationBounds(left = 0, top = 0, right = 4, bottom = 4), result?.bounds)
        assertEquals(0, alpha[7 * 8 + 7].toInt() and 0xFF)
        assertEquals(true, result?.changed)
    }

    @Test
    fun postProcessAlpha_everyPixelSurvives_reportsNoChangeAndCoversTheWholePlate() {
        // Given — 판 전체가 불투명하다. 침식을 켜도 프레임 테두리는 안 깎인다
        val alpha = ByteArray(64) { 255.toByte() }

        // When
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = AlphaPostProcessOptions(downscaleFactor = 1, areaOpeningMinPixels = 4),
        )

        // Then
        assertEquals(SegmentationBounds(left = 0, top = 0, right = 8, bottom = 8), result?.bounds)
        assertEquals(false, result?.changed)
        assertEquals(64L * 255, result?.alphaSum)
    }

    @Test
    fun postProcessAlpha_everythingIsSpeck_returnsNull() {
        // Given
        val alpha = ByteArray(64)
        alpha[0] = 255.toByte()

        // When
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = AlphaPostProcessOptions(downscaleFactor = 1, areaOpeningMinPixels = 4),
        )

        // Then
        assertNull(result)
    }

    @Test
    fun postProcessAlpha_everythingIsTransparent_returnsNull() {
        // Given
        val alpha = ByteArray(64)

        // When
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = AlphaPostProcessOptions(downscaleFactor = 1, areaOpeningMinPixels = 1),
        )

        // Then
        assertNull(result)
    }

    @Test
    fun postProcessAlpha_alphaLengthDoesNotMatch_failsFast() {
        // Given — 호출부가 어긋난 배열을 넘기면 엉뚱한 자리를 읽는다. 조용히 틀리지 않게 막는다
        val alpha = ByteArray(10)

        // When · Then
        assertFailsWith<IllegalArgumentException> { postProcessAlpha(alpha, width = 4, height = 4) }
    }

    @Test
    fun postProcessAlpha_cancelledMidway_propagatesTheCallersThrow() {
        // Given — 순수 커널에는 중단점이 없다. 콜백이 유일한 탈출구다
        val alpha = ByteArray(64) { 255.toByte() }
        var calls = 0

        // When · Then
        assertFailsWith<IllegalStateException> {
            postProcessAlpha(
                alpha,
                width = 8,
                height = 8,
                options = AlphaPostProcessOptions(downscaleFactor = 1, areaOpeningMinPixels = 4),
            ) {
                calls++
                if (calls > 2) error("cancelled")
            }
        }
    }

    @Test
    fun postProcessAlpha_downscaleFour_keepsTheBlobAndDropsTheDistantSpeck() {
        // Given — 32×32 에 16×16 덩어리와 멀리 떨어진 4×4 점
        val alpha = ByteArray(32 * 32)
        for (y in 0 until 16) for (x in 0 until 16) alpha[y * 32 + x] = 255.toByte()
        for (y in 28 until 32) for (x in 28 until 32) alpha[y * 32 + x] = 255.toByte()

        // When — 원본 환산 임계 64px 이면 축소판 임계는 4px 이다. 4×4 점은 축소판 1px 이라 죽는다
        val result = postProcessAlpha(
            alpha,
            width = 32,
            height = 32,
            options = AlphaPostProcessOptions(
                downscaleFactor = 4,
                areaOpeningMinPixels = 64,
                erodeEdge = false,
                minPixelsForDownscale = 0,
            ),
        )

        // Then
        assertEquals(SegmentationBounds(left = 0, top = 0, right = 16, bottom = 16), result?.bounds)
        assertEquals(0, alpha[31 * 32 + 31].toInt() and 0xFF)
    }

    @Test
    fun postProcessAlpha_sameSizeSpeckOnAndOffTheBlockGrid_isCountedDifferently() {
        // Given — 원본 4×4 잡티 둘. 하나는 블록에 정렬되고 하나는 한 칸 어긋났다.
        // OR 풀링이라 어긋난 쪽이 축소판에서 더 넓게 잡힌다 — 위상 슬롭을 여기 고정한다
        val aligned = ByteArray(32 * 32)
        for (y in 0 until 4) for (x in 0 until 4) aligned[y * 32 + x] = 255.toByte()

        val shifted = ByteArray(32 * 32)
        for (y in 1 until 5) for (x in 1 until 5) shifted[y * 32 + x] = 255.toByte()

        val options = AlphaPostProcessOptions(
            downscaleFactor = 4,
            areaOpeningMinPixels = 32,
            erodeEdge = false,
            minPixelsForDownscale = 0,
        )

        // When — 축소판 임계는 32 / 16 = 2 다. 정렬된 잡티는 1블록이라 죽고 어긋난 쪽은 4블록이라 산다
        val alignedResult = postProcessAlpha(aligned, width = 32, height = 32, options = options)
        val shiftedResult = postProcessAlpha(shifted, width = 32, height = 32, options = options)

        // Then
        assertNull(alignedResult)
        assertEquals(SegmentationBounds(left = 1, top = 1, right = 5, bottom = 5), shiftedResult?.bounds)
    }

    @Test
    fun postProcessAlpha_speckCloseToTheBlob_isNotRemovable() {
        // Given — 잡티가 실루엣에서 배율의 두 배 이내다. OR 풀링이 본체와 같은 성분으로 묶는다.
        // 제거할 수 없는 것이 한계이지 결함이 아니라는 사실을 여기 고정한다
        val alpha = ByteArray(32 * 32)
        for (y in 0 until 16) for (x in 0 until 16) alpha[y * 32 + x] = 255.toByte()
        alpha[17] = 255.toByte()

        // When
        val result = postProcessAlpha(
            alpha,
            width = 32,
            height = 32,
            options = AlphaPostProcessOptions(
                downscaleFactor = 4,
                areaOpeningMinPixels = 64,
                erodeEdge = false,
                minPixelsForDownscale = 0,
            ),
        )

        // Then — bounds 가 그 한 점까지 늘어난다
        assertEquals(18, result?.bounds?.right)
        assertEquals(255, alpha[17].toInt() and 0xFF)
    }

    @Test
    fun postProcessAlpha_sizeIsNotAMultipleOfFactor_keepsTheTrailingEdgeAlpha() {
        // Given — 33×33 전면 불투명. 축소를 내림하면 오른쪽·아래 한 줄이 판정에서 빠져 0이 된다
        val alpha = ByteArray(33 * 33) { 255.toByte() }

        // When
        val result = postProcessAlpha(
            alpha,
            width = 33,
            height = 33,
            options = AlphaPostProcessOptions(
                downscaleFactor = 4,
                areaOpeningMinPixels = 64,
                erodeEdge = false,
                minPixelsForDownscale = 0,
            ),
        )

        // Then
        assertEquals(SegmentationBounds(left = 0, top = 0, right = 33, bottom = 33), result?.bounds)
        assertEquals(255, alpha[33 * 33 - 1].toInt() and 0xFF)
    }

    @Test
    fun postProcessAlpha_belowTheDownscaleFloor_runsAtFactorOne() {
        // Given — 하한 미만이면 배율 1 로 돈다. 축소했다면 이 3픽셀 성분이 한 블록에 뭉쳐 살아남는다
        val alpha = ByteArray(64)
        alpha[0] = 255.toByte()
        alpha[1] = 255.toByte()
        alpha[8] = 255.toByte()

        // When
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = AlphaPostProcessOptions(
                downscaleFactor = 4,
                areaOpeningMinPixels = 4,
                minPixelsForDownscale = 1_000,
            ),
        )

        // Then
        assertNull(result)
    }

    @Test
    fun postProcessAlpha_atTheDownscaleFloor_runsAtTheConfiguredFactor() {
        // Given — 같은 입력인데 하한을 낮춰 축소가 발동하게 만든다
        val alpha = ByteArray(64)
        alpha[0] = 255.toByte()
        alpha[1] = 255.toByte()
        alpha[8] = 255.toByte()

        // When — 배율 4 면 세 픽셀이 한 블록에 뭉쳐 축소판 임계 1 을 넘는다
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = AlphaPostProcessOptions(
                downscaleFactor = 4,
                areaOpeningMinPixels = 4,
                erodeEdge = false,
                minPixelsForDownscale = 64,
            ),
        )

        // Then
        assertEquals(SegmentationBounds(left = 0, top = 0, right = 2, bottom = 2), result?.bounds)
    }

    @Test
    fun postProcessAlpha_nonSquarePlate_doesNotTransposeMaskDimensions() {
        // Given — 지금까지의 postProcessAlpha 테스트가 전부 정사각이라 maskWidth·maskHeight 를 뒤바꿔
        // 넘겨도 통과했다. 36×20 판(축소판 9×5)에서 원점과 먼 우하단 모서리 블록 하나만 불투명하게
        // 둔다 — 행 스트라이드가 뒤바뀌면 이 블록의 keep 인덱스가 다른 자리를 가리켜 통째로 지워진다
        val alpha = ByteArray(36 * 20)
        for (y in 16 until 20) for (x in 32 until 36) alpha[y * 36 + x] = 255.toByte()

        // When
        val result = postProcessAlpha(
            alpha,
            width = 36,
            height = 20,
            options = AlphaPostProcessOptions(
                downscaleFactor = 4,
                areaOpeningMinPixels = 1,
                erodeEdge = false,
                minPixelsForDownscale = 0,
            ),
        )

        // Then
        assertEquals(SegmentationBounds(left = 32, top = 16, right = 36, bottom = 20), result?.bounds)
        assertEquals(255, alpha[19 * 36 + 35].toInt() and 0xFF)
    }

    @Test
    fun postProcessAlpha_countingCancelledCallback_isCalledPastTheDownscaleStage() {
        // Given — 기존 취소 테스트는 세 번째 호출에서 던지는데 그 호출은 항상 downscaleMask 안에서
        // 난다. 나머지 단계의 checkCancelled 호출이 지워져도 그 테스트는 초록으로 남는다. 여기서는
        // 던지지 않고 세기만 해서 뒤 단계가 실제로 콜백을 부르는지를 잡는다
        val alpha = ByteArray(64) { 255.toByte() }
        var calls = 0

        // When
        postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = AlphaPostProcessOptions(downscaleFactor = 1, areaOpeningMinPixels = 4),
        ) {
            calls++
        }

        // Then — 8×8·배율1이면 각 단계가 정확히 행 수만큼(또는 그 -1) 부른다: downscaleMask 8 +
        // applyAreaOpening(행 쌍 union) 7 + dilateMask 8 + applyKeepMask 8 + erodeEdge 8 +
        // measureAlpha 8 = 47. 느슨한 하한(> 8)은 뒤 단계 다섯을 통째로 지워야만 걸리고 그중 하나만
        // 지워도(예: erodeEdge 의 호출 하나) 39로 여전히 8을 넘어 조용히 통과한다 — 그래서 정확한 값으로
        // 고정해 다섯 단계 중 어느 한 곳의 누락도 잡는다.
        // 다만 정확한 값은 단계 구성·순회 입도·픽스처 크기가 바뀌면 기능 회귀 없이도 깨진다.
        // 한 단계를 통째로 지웠을 때의 최대치는 area opening(7 손실)을 지운 40이다 — 그 위(> 40)를
        // 요구하면 여섯 단계 각각의 삭제(39·40·39·39·39·39)를 전부 잡으면서 유지비도 준다
        assertTrue(calls > 40)
    }

    @Test
    fun postProcessAlpha_refineEnabledWithGuidance_producesPartialAlpha() {
        // Given — 하드 매트에는 부분 알파가 없다
        val alpha = notchedMask()
        val options = AlphaPostProcessOptions(
            downscaleFactor = 1,
            areaOpeningMinPixels = 4,
            erodeEdge = false,
            refineEdges = true,
            refineDownscale = 1,
            refineRadius = 2,
        )

        // When
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = options,
            guidance = splitGuidance,
        )

        // Then
        assertEquals(true, result?.changed)
        assertTrue((result?.partialAlphaPixels ?: 0) > 0, "partial=${result?.partialAlphaPixels}")
    }

    @Test
    fun postProcessAlpha_refineDisabled_leavesTheAlphaUntouchedByRefinement() {
        // Given — 같은 입력·같은 안내자로 켜고 끈다. 플래그가 무시되면 두 결과가 같아진다
        val enabled = notchedMask()
        val disabled = notchedMask()
        val base = AlphaPostProcessOptions(
            downscaleFactor = 1,
            areaOpeningMinPixels = 4,
            erodeEdge = false,
            refineDownscale = 1,
            refineRadius = 2,
        )

        // When
        postProcessAlpha(
            enabled,
            width = 8,
            height = 8,
            options = base.copy(refineEdges = true),
            guidance = splitGuidance,
        )
        postProcessAlpha(
            disabled,
            width = 8,
            height = 8,
            options = base.copy(refineEdges = false),
            guidance = splitGuidance,
        )

        // Then
        assertTrue(
            !enabled.asInts().contentEquals(disabled.asInts()),
            "refineEdges 가 무시됐다 enabled=${enabled.asInts().toList()}",
        )
    }

    @Test
    fun postProcessAlpha_refineEnabledWithoutGuidance_skipsRefinement() {
        // Given — 안내자를 못 대는 호출부가 커널을 그대로 쓸 수 있어야 한다
        val withoutGuidance = notchedMask()
        val disabled = notchedMask()
        val base = AlphaPostProcessOptions(
            downscaleFactor = 1,
            areaOpeningMinPixels = 4,
            erodeEdge = false,
            refineDownscale = 1,
            refineRadius = 2,
        )

        // When
        postProcessAlpha(withoutGuidance, width = 8, height = 8, options = base.copy(refineEdges = true))
        postProcessAlpha(disabled, width = 8, height = 8, options = base.copy(refineEdges = false))

        // Then
        assertContentEquals(disabled.asInts(), withoutGuidance.asInts())
    }
}
