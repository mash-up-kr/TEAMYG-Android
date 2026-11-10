package com.teamyg.parfait.feature.camera.impl.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** `CameraFeedLayer`와 같은 순서(탭 → 핀치)로 붙여, 두 입력이 한 제스처를 나눠 갖는 계약 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class PinchZoomInputTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var isShown by mutableStateOf(true)
    private var startCount = 0
    private var endCount = 0
    private var tapCount = 0
    private val scales = mutableListOf<Float>()

    @Test
    fun pinchOut_reportsZoomInBetweenOneStartAndEnd() {
        setInput()

        composeTestRule.onRoot().performTouchInput {
            pinch(
                start0 = center - Offset(50f, 0f),
                end0 = center - Offset(300f, 0f),
                start1 = center + Offset(50f, 0f),
                end1 = center + Offset(300f, 0f),
            )
        }

        composeTestRule.runOnIdle {
            assertEquals(1, startCount)
            assertEquals(1, endCount)
            assertTrue(scales.fold(1f) { acc, scale -> acc * scale } > 1f)
        }
    }

    @Test
    fun oneFinger_tapAndSwipe_areNotPinchAndNotConsumed() {
        setInput()

        composeTestRule.onRoot().performTouchInput { click(center) }
        composeTestRule.onRoot().performTouchInput { swipe(center, center + Offset(300f, 0f)) }

        composeTestRule.runOnIdle {
            // detectTapGestures는 소비되지 않은 이동을 탭으로 끝낸다 — 스와이프도 탭이면 핀치가 소비하지 않았다
            assertEquals(2, tapCount)
            assertEquals(0, startCount)
            assertTrue(scales.isEmpty())
        }
    }

    @Test
    fun twoFingerJitterBelowTouchSlop_doesNotZoom() {
        setInput()

        composeTestRule.onRoot().performTouchInput {
            down(0, center - Offset(100f, 0f))
            down(1, center + Offset(100f, 0f))
            moveTo(1, center + Offset(102f, 0f))
            up(1)
            up(0)
        }

        composeTestRule.runOnIdle {
            assertEquals(1, startCount)
            assertEquals(1, endCount)
            assertTrue(scales.isEmpty())
        }
    }

    @Test
    fun liftingOneFingerAndPlacingAgain_startsNewPinch() {
        setInput()

        composeTestRule.onRoot().performTouchInput {
            down(0, center - Offset(100f, 0f))
            down(1, center + Offset(100f, 0f))
            moveTo(1, center + Offset(300f, 0f))
            up(1)
            down(1, center + Offset(100f, 0f))
            moveTo(1, center + Offset(300f, 0f))
            up(1)
            up(0)
        }

        composeTestRule.runOnIdle {
            assertEquals(2, startCount)
            assertEquals(2, endCount)
        }
    }

    @Test
    fun cancelledMidPinch_stillReportsEndOnce() {
        setInput()

        composeTestRule.onRoot().performTouchInput {
            down(0, center - Offset(100f, 0f))
            down(1, center + Offset(100f, 0f))
        }
        composeTestRule.waitForIdle()
        isShown = false

        composeTestRule.runOnIdle {
            assertEquals(1, startCount)
            assertEquals(1, endCount)
        }
    }

    private fun setInput() {
        composeTestRule.setContent {
            if (isShown) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { tapCount++ } }
                        .pointerInput(Unit) {
                            detectPinchZoom(
                                onPinchZoomStart = { startCount++ },
                                onPinchZoom = { scales += it },
                                onPinchZoomEnd = { endCount++ },
                            )
                        },
                )
            }
        }
    }
}
