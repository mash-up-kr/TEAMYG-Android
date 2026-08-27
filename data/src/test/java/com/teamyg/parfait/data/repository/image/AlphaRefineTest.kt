package com.teamyg.parfait.data.repository.image

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** 값이 0~1 범위라 이 정도면 충분하다 */
private const val TOLERANCE = 1e-4f

private fun assertClose(
    expected: Float,
    actual: Float,
    message: String = "",
) {
    assertTrue(abs(expected - actual) <= TOLERANCE, "$message expected=$expected actual=$actual")
}

/** 회색 픽셀. 휘도가 `value / 255` 다 */
private fun gray(value: Int): Int = (0xFF shl 24) or (value shl 16) or (value shl 8) or value

/** 왼쪽 [darkColumns] 칸이 검고 나머지가 흰 안내자 */
private fun splitGuidance(
    width: Int,
    height: Int,
    darkColumns: Int,
) = IntArray(width * height) { index -> if (index % width < darkColumns) gray(0) else gray(255) }

/** [opaqueFrom] 칸부터 오른쪽 끝까지 불투명한 알파 */
private fun maskFrom(
    width: Int,
    height: Int,
    opaqueFrom: Int,
) = ByteArray(width * height) { index -> if (index % width >= opaqueFrom) 255.toByte() else 0 }

class AlphaRefineTest {
    @Test
    fun boxMean_everyValueIsOne_staysOneEvenAtTheCorners() = runTest {
        // Given — 고정 개수로 나누면 모서리가 4/9 로 내려앉는다
        val src = FloatArray(9) { 1f }

        // When
        val mean = boxMean(src, width = 3, height = 3, radius = 1)

        // Then
        for (index in mean.indices) assertClose(1f, mean[index], "index=$index")
    }

    @Test
    fun boxMean_radiusLargerThanTheArray_averagesEverything() = runTest {
        // Given
        val src = floatArrayOf(0f, 1f, 0f, 1f)

        // When
        val mean = boxMean(src, width = 2, height = 2, radius = 5)

        // Then
        for (index in mean.indices) assertClose(0.5f, mean[index], "index=$index")
    }

    @Test
    fun boxMean_singleSpike_spreadsOverTheWindowOnly() = runTest {
        // Given — 5×5 한가운데(2,2)만 1 이다
        val src = FloatArray(25)
        src[12] = 1f

        // When
        val mean = boxMean(src, width = 5, height = 5, radius = 1)

        // Then — 중앙과 대각 이웃(1,1)은 창이 온전히 안에 들어 1/9, 두 칸 밖은 0
        assertClose(1f / 9f, mean[12], "center")
        assertClose(1f / 9f, mean[6], "diagonal neighbour")
        assertClose(0f, mean[0], "two cells away")
    }

    @Test
    fun boxMean_windowClippedAtTheEdge_usesTheActualCount() = runTest {
        // Given — 한 행짜리. 왼쪽 끝의 창은 두 칸만 포함한다
        val src = floatArrayOf(1f, 0f, 0f, 0f)

        // When
        val mean = boxMean(src, width = 4, height = 1, radius = 1)

        // Then
        assertClose(0.5f, mean[0], "left edge counts two cells")
        assertClose(1f / 3f, mean[1], "interior counts three cells")
    }

    @Test
    fun downscaleLuminance_greenAndBlueWeights_areNotSwapped() = runTest {
        // Given — 순수색 셋. 계수를 맞바꾸면 회색·빨강 테스트로는 안 잡힌다
        val pixels = intArrayOf(0xFF00FF00.toInt(), 0xFF0000FF.toInt(), 0xFFFF0000.toInt())

        // When
        val sub = downscaleLuminance(pixels, width = 3, height = 1, factor = 1)

        // Then
        assertClose(0.587f, sub[0], "green")
        assertClose(0.114f, sub[1], "blue")
        assertClose(0.299f, sub[2], "red")
    }

    @Test
    fun downscaleLuminance_ignoresTheAlphaChannel() = runTest {
        // Given — 같은 빨강인데 알파만 다르다. 안내자는 색만 봐야 한다
        val pixels = intArrayOf(0xFFFF0000.toInt(), 0x00FF0000)

        // When
        val sub = downscaleLuminance(pixels, width = 2, height = 1, factor = 1)

        // Then
        assertClose(0.299f, sub[0], "opaque red")
        assertClose(0.299f, sub[1], "transparent red")
    }

    @Test
    fun downscaleAlpha_valueAbove127_isNotMisreadAsNegative() = runTest {
        // Given — 부호 처리를 빠뜨리면 음수가 된다
        val alpha = byteArrayOf(0, 128.toByte(), 255.toByte())

        // When
        val sub = downscaleAlpha(alpha, width = 3, height = 1, factor = 1)

        // Then
        assertClose(0f, sub[0], "transparent")
        assertClose(128f / 255f, sub[1], "half")
        assertClose(1f, sub[2], "opaque")
    }

    @Test
    fun downscaleAlpha_widthIsNotAMultipleOfFactor_averagesTheShortBlock() = runTest {
        // Given — 3×1 을 배율 2 로 줄이면 두 번째 블록에 한 칸만 든다
        val alpha = byteArrayOf(255.toByte(), 0, 255.toByte())

        // When
        val sub = downscaleAlpha(alpha, width = 3, height = 1, factor = 2)

        // Then
        assertEquals(2, sub.size)
        assertClose(0.5f, sub[0], "full block")
        assertClose(1f, sub[1], "short block averages one cell, not two")
    }

    @Test
    fun downscaleAlpha_heightIsNotAMultipleOfFactor_keepsTheTrailingRow() = runTest {
        // Given — 3×3 을 배율 2 로 줄이면 2×2 다. 마지막 행·열은 한 칸짜리 블록이다.
        // 아래 행만 불투명하게 두면 세로 인덱싱이 틀렸을 때 값이 어긋난다
        val alpha = ByteArray(9) { index -> if (index >= 6) 255.toByte() else 0 }

        // When
        val sub = downscaleAlpha(alpha, width = 3, height = 3, factor = 2)

        // Then
        assertEquals(4, sub.size)
        assertClose(0f, sub[0], "top-left block has no opaque row")
        assertClose(0f, sub[1], "top-right block has no opaque row")
        assertClose(1f, sub[2], "bottom-left block is the trailing row")
        assertClose(1f, sub[3], "bottom-right block is the trailing row")
    }

    @Test
    fun guidedCoefficients_constantGuidance_degeneratesToADoubleMean() = runTest {
        // Given — 안내자에 경계가 없으면 알파를 옮길 근거가 없다. a 는 0 이고 b 만 남는다
        val guidance = FloatArray(16) { 0.5f }
        val input = FloatArray(16) { index -> if (index % 4 < 2) 1f else 0f }

        // When
        val coefficients = guidedCoefficients(
            guidance = guidance,
            input = input,
            width = 4,
            height = 4,
            radius = 1,
            epsilon = 1e-4f,
        )

        // Then — b 는 창 평균을 **두 번** 거친 값이다. 한 번만 기대하면 가장자리에서 0.167 어긋난다
        val once = boxMean(input, width = 4, height = 4, radius = 1)
        val twice = boxMean(once, width = 4, height = 4, radius = 1)
        for (index in coefficients.a.indices) {
            assertClose(0f, coefficients.a[index], "a index=$index")
            assertClose(twice[index], coefficients.b[index], "b index=$index")
        }
    }

    @Test
    fun guidedCoefficients_inputEqualsGuidance_reproducesTheGuidance() = runTest {
        // Given — p 가 I 와 같으면 q = I 여야 하므로 a = 1, b = 0 이다.
        // ⚠️ 안내자는 **모든 창에 분산이 있어야** 한다. 계단형이면 가장자리 창의 분산이 0 이라
        // 그 자리 a 가 0 으로 떨어지고 두 번째 평균이 그것을 안쪽까지 번지게 한다
        val guidance = FloatArray(16) { index -> (index % 4) * 0.3f }

        // When
        val coefficients = guidedCoefficients(
            guidance = guidance,
            input = guidance.copyOf(),
            width = 4,
            height = 4,
            radius = 1,
            epsilon = 1e-8f,
        )

        // Then
        for (index in coefficients.a.indices) {
            assertClose(1f, coefficients.a[index], "a index=$index")
            assertClose(0f, coefficients.b[index], "b index=$index")
        }
    }

    @Test
    fun applyCoefficients_identityCoefficients_writeTheGuidanceAsAlpha() = runTest {
        // Given — a = 1, b = 0 이면 알파가 안내자 휘도 그대로여야 한다
        val alpha = ByteArray(4)
        val guidance = intArrayOf(gray(0), gray(128), gray(255), gray(64))
        val coefficients = GuidedCoefficients(a = FloatArray(4) { 1f }, b = FloatArray(4))

        // When
        val changed = applyCoefficients(
            alpha = alpha,
            guidance = guidance,
            coefficients = coefficients,
            width = 2,
            height = 2,
            subWidth = 2,
            subHeight = 2,
            factor = 1,
        )

        // Then
        assertEquals(true, changed)
        assertEquals(0, alpha[0].toInt() and 0xFF)
        assertEquals(128, alpha[1].toInt() and 0xFF)
        assertEquals(255, alpha[2].toInt() and 0xFF)
        assertEquals(64, alpha[3].toInt() and 0xFF)
    }

    @Test
    fun applyCoefficients_resultMatchesTheCurrentAlpha_reportsNoChange() = runTest {
        // Given — 호출부가 이 값으로 원본 판 재사용을 판정한다. 늘 참이면 그 경로가 죽는다
        val alpha = ByteArray(2) { 255.toByte() }
        val guidance = intArrayOf(gray(255), gray(255))
        val coefficients = GuidedCoefficients(a = FloatArray(2) { 1f }, b = FloatArray(2))

        // When
        val changed = applyCoefficients(
            alpha = alpha,
            guidance = guidance,
            coefficients = coefficients,
            width = 2,
            height = 1,
            subWidth = 2,
            subHeight = 1,
            factor = 1,
        )

        // Then
        assertEquals(false, changed)
    }

    @Test
    fun applyCoefficients_outOfRangeResult_isClampedInsteadOfWrapping() = runTest {
        // Given — 자르지 않으면 바이트가 감겨 반대 값이 된다
        val alpha = ByteArray(2)
        val guidance = intArrayOf(gray(255), gray(255))
        val coefficients = GuidedCoefficients(a = floatArrayOf(4f, -4f), b = FloatArray(2))

        // When
        applyCoefficients(
            alpha = alpha,
            guidance = guidance,
            coefficients = coefficients,
            width = 2,
            height = 1,
            subWidth = 2,
            subHeight = 1,
            factor = 1,
        )

        // Then
        assertEquals(255, alpha[0].toInt() and 0xFF)
        assertEquals(0, alpha[1].toInt() and 0xFF)
    }

    @Test
    fun applyCoefficients_verticallyUpscaledCoefficients_interpolateBetweenRows() = runTest {
        // Given — 세로로만 변하는 계수. 세로 보간을 빠뜨리면 계단 둘만 나온다
        val alpha = ByteArray(8)
        val guidance = IntArray(8) { gray(255) }
        val coefficients = GuidedCoefficients(a = FloatArray(2), b = floatArrayOf(0f, 1f))

        // When
        applyCoefficients(
            alpha = alpha,
            guidance = guidance,
            coefficients = coefficients,
            width = 1,
            height = 8,
            subWidth = 1,
            subHeight = 2,
            factor = 4,
        )

        // Then
        val values = IntArray(8) { alpha[it].toInt() and 0xFF }
        for (index in 1 until 8) {
            assertTrue(values[index] >= values[index - 1], "index=$index values=${values.toList()}")
        }
        assertTrue(values.toSet().size > 2, "nearest 되올림이면 값이 둘뿐이다 values=${values.toList()}")
    }

    @Test
    fun applyCoefficients_horizontallyUpscaledCoefficients_interpolateBetweenColumns() = runTest {
        // Given — 가로로만 변하는 계수. `subWidth = 1` 인 세로 테스트는 이 축을 전혀 보지 못한다
        val alpha = ByteArray(8)
        val guidance = IntArray(8) { gray(255) }
        val coefficients = GuidedCoefficients(a = FloatArray(2), b = floatArrayOf(0f, 1f))

        // When
        applyCoefficients(
            alpha = alpha,
            guidance = guidance,
            coefficients = coefficients,
            width = 8,
            height = 1,
            subWidth = 2,
            subHeight = 1,
            factor = 4,
        )

        // Then — b 가 좌우 대칭(0→1)이므로 되올린 값도 중심에 대해 대칭이어야 한다.
        // nearest 되올림이나 반픽셀 정렬 삭제는 이 대칭을 깬다
        val values = IntArray(8) { alpha[it].toInt() and 0xFF }
        for (index in 0 until 4) {
            assertEquals(
                255,
                values[index] + values[7 - index],
                "index=$index values=${values.toList()}",
            )
        }
    }

    @Test
    fun refineAlpha_maskOverhangsIntoTheDarkSide_pullsTheEdgeBackToTheColourEdge() = runTest {
        // Given — 색 경계는 16 인데 마스크가 13 까지 넘어와 배경 3칸을 물고 있다
        val guided = maskFrom(width = 32, height = 8, opaqueFrom = 13)
        val flat = maskFrom(width = 32, height = 8, opaqueFrom = 13)

        // When — 같은 마스크를 색 경계가 있는 안내자와 균일한 안내자로 각각 정련한다
        refineAlpha(
            alpha = guided,
            guidance = splitGuidance(width = 32, height = 8, darkColumns = 16),
            width = 32,
            height = 8,
            downscale = 1,
            radius = 4,
            epsilon = 1e-4f,
        )
        refineAlpha(
            alpha = flat,
            guidance = IntArray(32 * 8) { gray(255) },
            width = 32,
            height = 8,
            downscale = 1,
            radius = 4,
            epsilon = 1e-4f,
        )

        // Then — 배경을 물고 있던 자리가 색 안내자 쪽에서 더 투명해진다.
        // 안내자를 무시하는 구현이면 두 값이 같아 이 단언이 깨진다
        val overhang = 4 * 32 + 14
        assertTrue(
            (guided[overhang].toInt() and 0xFF) < (flat[overhang].toInt() and 0xFF),
            "guided=${guided[overhang].toInt() and 0xFF} flat=${flat[overhang].toInt() and 0xFF}",
        )
    }

    @Test
    fun refineAlpha_insideTheSubject_staysOpaque() = runTest {
        // Given — 정련이 내부까지 반투명하게 만들면 안 된다.
        // 탐침 자리를 경계에서 창 하나 안쪽(20)에 둔다 — 끝(28)에 두면 계수가 평탄해져
        // 마지막 창 평균을 지우는 변이를 못 잡는다
        val alpha = maskFrom(width = 32, height = 8, opaqueFrom = 13)

        // When
        refineAlpha(
            alpha = alpha,
            guidance = splitGuidance(width = 32, height = 8, darkColumns = 16),
            width = 32,
            height = 8,
            downscale = 1,
            radius = 4,
            epsilon = 1e-4f,
        )

        // Then
        val inside = 4 * 32 + 20
        assertTrue((alpha[inside].toInt() and 0xFF) > 250, "value=${alpha[inside].toInt() and 0xFF}")
    }

    @Test
    fun refineAlpha_misalignedHardEdge_becomesASoftTransition() = runTest {
        // Given — 이 라운드의 목적이다. 정련 전에는 0 과 255 뿐이다.
        // ⚠️ 마스크 경계를 색 경계와 어긋나게 둔다. 같은 자리(p ≡ I)면 가이드 필터는 경계를
        // **일부러 보존하므로** 부분 알파가 생기지 않는다
        val alpha = maskFrom(width = 32, height = 8, opaqueFrom = 13)

        // When
        refineAlpha(
            alpha = alpha,
            guidance = splitGuidance(width = 32, height = 8, darkColumns = 16),
            width = 32,
            height = 8,
            downscale = 1,
            radius = 4,
            epsilon = 1e-4f,
        )

        // Then
        val partial = alpha.count { (it.toInt() and 0xFF) in 1..254 }
        assertTrue(partial > 0, "partial=$partial")
    }

    @Test
    fun refineAlpha_downscaledCoefficients_keepTheEdgeAtTheSamePlace() = runTest {
        // Given — 계수를 축소판에서 구해도 경계 위치가 밀리면 안 된다.
        // 두 설정의 유효 창은 같지 않다(원본 기준 9 대 12). 그래도 경계는 2칸 안에 들어야 한다
        val fullScale = maskFrom(width = 64, height = 16, opaqueFrom = 28)
        val downscaled = maskFrom(width = 64, height = 16, opaqueFrom = 28)
        val guidance = splitGuidance(width = 64, height = 16, darkColumns = 32)

        // When
        refineAlpha(
            alpha = fullScale,
            guidance = guidance,
            width = 64,
            height = 16,
            downscale = 1,
            radius = 4,
            epsilon = 1e-4f,
        )
        refineAlpha(
            alpha = downscaled,
            guidance = guidance,
            width = 64,
            height = 16,
            downscale = 4,
            radius = 1,
            epsilon = 1e-4f,
        )

        // Then — 가운데 행에서 알파가 128 을 넘는 첫 칸이 두 칸 이상 어긋나지 않는다
        val row = 8 * 64
        val fullCrossing = (0 until 64).first { (fullScale[row + it].toInt() and 0xFF) > 128 }
        val downCrossing = (0 until 64).first { (downscaled[row + it].toInt() and 0xFF) > 128 }
        assertTrue(abs(fullCrossing - downCrossing) <= 2, "full=$fullCrossing down=$downCrossing")
    }

    @Test
    fun boxMean_cancelledMidway_throws() {
        // Given
        val src = FloatArray(16) { 1f }
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) { boxMean(src, width = 4, height = 4, radius = 1) }
        }
    }

    @Test
    fun guidedCoefficients_cancelledMidway_throws() {
        // Given
        val guidance = FloatArray(16) { 0.5f }
        val input = FloatArray(16) { 0.5f }
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) {
                guidedCoefficients(guidance, input, width = 4, height = 4, radius = 1, epsilon = 1e-4f)
            }
        }
    }

    @Test
    fun applyCoefficients_cancelledMidway_throws() {
        // Given
        val alpha = ByteArray(16) { 255.toByte() }
        val guidance = IntArray(16) { 0xFF808080.toInt() }
        val coefficients = GuidedCoefficients(a = FloatArray(16), b = FloatArray(16) { 1f })
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) {
                applyCoefficients(
                    alpha = alpha,
                    guidance = guidance,
                    coefficients = coefficients,
                    width = 4,
                    height = 4,
                    subWidth = 4,
                    subHeight = 4,
                    factor = 1,
                )
            }
        }
    }

    @Test
    fun refineAlpha_countingChecks_visitsEveryStage() {
        // Given — 4×4·배율1·반경1. boxMean 여섯 번(각 행·열 두 루프)·downscale 둘·계수 루프·
        // applyCoefficients 가 각각 확인한다
        val alpha = ByteArray(16) { 255.toByte() }
        val guidance = IntArray(16) { 0xFF808080.toInt() }
        val job = CountingJob()

        // When
        runKernelCounting(job) {
            refineAlpha(
                alpha = alpha,
                guidance = guidance,
                width = 4,
                height = 4,
                downscale = 1,
                radius = 1,
                epsilon = 1e-4f,
            )
        }

        // Then — 실제 값을 재서 채우고, 한 단계를 통째로 지웠을 때의 최대치 위로 하한을 건다
        assertTrue(job.calls > MEASURED_LOWER_BOUND)
    }

    private companion object {
        /**
         * 4×4·배율1·반경1 에서 실측한 총합은 64. 확인 지점을 하나씩 지우고 잰 총합은
         * downscale 56, boxMean 행 루프 40, boxMean 열 루프 40, guidedCoefficients 계수 루프 60,
         * applyCoefficients 루프 60 이다. 그중 최댓값(60)을 하한으로 쓴다 — 어느 한 확인 지점을
         * 지워도 총합이 60 이하로 떨어져 이 하한에 잡힌다
         */
        const val MEASURED_LOWER_BOUND = 60
    }
}
