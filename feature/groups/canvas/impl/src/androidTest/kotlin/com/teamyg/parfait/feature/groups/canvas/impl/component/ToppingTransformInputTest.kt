package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

private const val LAYER_TAG = "layer"

/** [at] 이 px 로 바꾸는 좌표 단위. 밀도가 달라도 손가락 폭 비율이 유지된다 */
private const val LAYER_UNITS = 400f
private val LAYER_SIZE = 300.dp
private const val STEP_COUNT = 12

private const val PAN_TOLERANCE_PX = 1f
private const val ZOOM_TOLERANCE = 0.05f
private const val ROTATION_TOLERANCE_DEGREES = 1f

private data class Transform(
    val pan: Offset,
    val zoom: Float,
    val rotationDelta: Float,
)

/** 화면과 같은 순서(`toppingTapInput` 바깥, `toppingTransformInput` 안쪽)로 붙인 두 입력의 계약 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ToppingTransformInputTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val transforms = mutableListOf<Transform>()
    private val activeChanges = mutableListOf<Boolean>()
    private var hitCount = 0
    private var missCount = 0
    private var unit = 1f
    private var maxJump = 0f
    private var minSpan = 0f

    @Test
    fun singleFinger_onTarget_doesNotTransform() {
        setLayer()

        drag(from = at(200f, 200f), to = at(260f, 200f))

        composeTestRule.runOnIdle { assertEquals(emptyList<Transform>(), transforms) }
    }

    @Test
    fun twoFingers_outsideTarget_zoomAndRotate() {
        setLayer()

        composeTestRule.onNodeWithTag(LAYER_TAG).performTouchInput {
            down(0, at(100f, 200f))
            down(1, at(300f, 200f))
            moveBothTo(at(59f, 59f), at(341f, 341f))
            up(0)
            up(1)
        }

        composeTestRule.runOnIdle {
            val zoom = transforms.fold(1f) { acc, it -> acc * it.zoom }
            val rotation = transforms.sumOf { it.rotationDelta.toDouble() }.toFloat()
            assertEquals(2f, zoom, ZOOM_TOLERANCE)
            assertEquals(45f, rotation, ROTATION_TOLERANCE_DEGREES)
        }
    }

    @Test
    fun fingersCloserThanMinSpan_doNotTransform() {
        setLayer()

        composeTestRule.onNodeWithTag(LAYER_TAG).performTouchInput {
            down(0, at(100f, 100f))
            down(1, at(100f + minSpan / 2f, 100f))
            moveBothTo(at(160f, 190f), at(160f + minSpan * 0.8f, 150f))
            up(0)
            up(1)
        }

        composeTestRule.runOnIdle { assertEquals(emptyList<Transform>(), transforms) }
    }

    @Test
    fun pinchThroughNearlyTouching_keepsZoomConsistent() {
        setLayer()

        composeTestRule.onNodeWithTag(LAYER_TAG).performTouchInput {
            down(0, at(50f, 200f))
            down(1, at(350f, 200f))
            // 거의 붙을 만큼 오므렸다가 다시 벌린다
            moveBothTo(at(195f, 200f), at(205f, 200f))
            moveBothTo(at(50f, 200f), at(350f, 200f))
            up(0)
            up(1)
        }

        composeTestRule.runOnIdle {
            val zoom = transforms.fold(1f) { acc, it -> acc * it.zoom }
            assertEquals(1f, zoom, ZOOM_TOLERANCE)
        }
    }

    @Test
    fun singlePointerJump_isDropped() {
        setLayer()

        composeTestRule.onNodeWithTag(LAYER_TAG).performTouchInput {
            down(0, at(100f, 200f))
            down(1, at(300f, 200f))
            up(0)
            moveTo(1, at(300f, 200f + maxJump * 2))
            moveTo(1, at(305f, 200f + maxJump * 2))
            up(1)
        }

        composeTestRule.runOnIdle { assertPanSum(expected = at(5f, 0f)) }
    }

    @Test
    fun pointerCountChange_doesNotJumpPan() {
        setLayer()

        val step = 5f
        composeTestRule.onNodeWithTag(LAYER_TAG).performTouchInput {
            // 한 손가락 +x 80px 은 옮기지 않는다
            down(0, at(200f, 200f))
            repeat(16) { i -> moveTo(0, at(200f + step * (i + 1), 200f)) }
            // 두 번째 손가락을 대고 둘 다 +y 로 40px
            down(1, at(80f, 330f))
            repeat(8) { i ->
                updatePointerTo(0, at(280f, 200f + step * (i + 1)))
                updatePointerTo(1, at(80f, 330f + step * (i + 1)))
                move()
            }
            // 첫 손가락을 떼고 남은 손가락만 +x 로 20px
            up(0)
            repeat(4) { i -> moveTo(1, at(80f + step * (i + 1), 370f)) }
            up(1)
        }

        composeTestRule.runOnIdle {
            assertPanSum(expected = at(20f, 40f))
            assertTrue(transforms.all { it.pan.getDistance() <= step * 2 * unit })
        }
    }

    @Test
    fun twoFingerTapWithoutMove_doesNotFireTap() {
        setLayer()

        composeTestRule.onNodeWithTag(LAYER_TAG).performTouchInput {
            down(0, at(50f, 50f))
            down(1, at(350f, 350f))
            up(0)
            up(1)
        }

        composeTestRule.runOnIdle {
            assertEquals(0, hitCount)
            assertEquals(0, missCount)
        }
    }

    @Test
    fun thirdPointer_isIgnored() {
        setLayer()

        var beforeThird = 0
        composeTestRule.onNodeWithTag(LAYER_TAG).performTouchInput {
            down(0, at(100f, 200f))
            down(1, at(300f, 200f))
            moveBothTo(at(80f, 200f), at(320f, 200f))
        }
        composeTestRule.runOnIdle { beforeThird = transforms.size }
        composeTestRule.onNodeWithTag(LAYER_TAG).performTouchInput {
            down(2, at(200f, 100f))
            repeat(STEP_COUNT) { i ->
                val fraction = (i + 1).toFloat() / STEP_COUNT
                moveTo(2, lerp(at(200f, 100f), at(50f, 380f), fraction))
            }
            up(2)
            up(0)
            up(1)
        }

        composeTestRule.runOnIdle {
            transforms.drop(beforeThird).forEach {
                assertEquals(1f, it.zoom, 0.01f)
                assertEquals(0f, it.rotationDelta, 0.5f)
            }
        }
    }

    @Test
    fun disabled_doesNothing() {
        setLayer(enabled = { false })

        composeTestRule.onNodeWithTag(LAYER_TAG).performTouchInput {
            down(0, at(100f, 200f))
            down(1, at(300f, 200f))
            moveBothTo(at(59f, 59f), at(341f, 341f))
            up(0)
            up(1)
        }

        composeTestRule.runOnIdle {
            assertEquals(emptyList<Transform>(), transforms)
            assertEquals(emptyList<Boolean>(), activeChanges)
        }
    }

    @Test
    fun twoFingersWithoutMove_doesNotTransform() {
        setLayer()

        composeTestRule.onNodeWithTag(LAYER_TAG).performTouchInput {
            down(0, at(50f, 50f))
            down(1, at(350f, 350f))
            repeat(STEP_COUNT) { move() }
            up(0)
            up(1)
        }

        composeTestRule.runOnIdle { assertEquals(emptyList<Transform>(), transforms) }
    }

    @Test
    fun gestureActive_spansFirstDownToLastUp() {
        setLayer()

        composeTestRule.onNodeWithTag(LAYER_TAG).performTouchInput { down(0, at(50f, 50f)) }
        composeTestRule.runOnIdle { assertEquals(listOf(true), activeChanges) }

        composeTestRule.onNodeWithTag(LAYER_TAG).performTouchInput {
            down(1, at(350f, 350f))
            up(0)
            up(1)
        }

        composeTestRule.runOnIdle { assertEquals(listOf(true, false), activeChanges) }
    }

    private fun setLayer(enabled: () -> Boolean = { true }) {
        composeTestRule.setContent {
            with(LocalDensity.current) {
                unit = LAYER_SIZE.toPx() / LAYER_UNITS
                maxJump = TOPPING_POINTER_MAX_JUMP.toPx() / unit
                minSpan = TOPPING_PINCH_MIN_SPAN.toPx() / unit
            }
            Box(
                modifier = Modifier
                    .requiredSize(LAYER_SIZE)
                    .testTag(LAYER_TAG)
                    .toppingTapInput(
                        entries = { listOf(Unit to target()) },
                        keyOf = { it },
                        onHit = { hitCount++ },
                        onMiss = { missCount++ },
                    ).toppingTransformInput(
                        enabled = enabled,
                        onTransform = { pan, zoom, rotationDelta ->
                            transforms += Transform(pan, zoom, rotationDelta)
                        },
                        onGestureActiveChange = { activeChanges += it },
                    ),
            )
        }
    }

    /** 탭 판정용. 중심 (200,200), 100×100 단위 사각형 */
    private fun target() = ToppingHitTarget(
        centerXPx = 200f * unit,
        centerYPx = 200f * unit,
        imageWidthPx = 100f * unit,
        imageHeightPx = 100f * unit,
        rotationDegrees = 0f,
        borderWidthPx = 0f,
        outline = null,
    )

    private fun at(
        x: Float,
        y: Float,
    ) = Offset(x * unit, y * unit)

    private fun drag(
        from: Offset,
        to: Offset,
    ) {
        composeTestRule.onNodeWithTag(LAYER_TAG).performTouchInput {
            down(0, from)
            repeat(STEP_COUNT) { i -> moveTo(0, lerp(from, to, (i + 1).toFloat() / STEP_COUNT)) }
            up(0)
        }
    }

    /** 포인터 0·1 을 현재 자리에서 목표까지 같은 이벤트로 함께 옮긴다 */
    private fun TouchInjectionScope.moveBothTo(
        target0: Offset,
        target1: Offset,
    ) {
        val start0 = currentPosition(0)!!
        val start1 = currentPosition(1)!!
        repeat(STEP_COUNT) { i ->
            val fraction = (i + 1).toFloat() / STEP_COUNT
            updatePointerTo(0, lerp(start0, target0, fraction))
            updatePointerTo(1, lerp(start1, target1, fraction))
            move()
        }
    }

    private fun assertPanSum(expected: Offset) {
        val sum = transforms.fold(Offset.Zero) { acc, it -> acc + it.pan }
        assertTrue(
            "pan 합 $sum, 기대 $expected",
            abs(sum.x - expected.x) <= PAN_TOLERANCE_PX && abs(sum.y - expected.y) <= PAN_TOLERANCE_PX,
        )
    }

    private fun lerp(
        start: Offset,
        stop: Offset,
        fraction: Float,
    ): Offset = start + (stop - start) * fraction
}
