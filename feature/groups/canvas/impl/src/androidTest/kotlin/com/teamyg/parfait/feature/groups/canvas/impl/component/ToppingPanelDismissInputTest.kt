package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 어느 기기 화면이든 덮고 남는 크기 */
private const val TARGET_SIZE_PX = 100_000f

/** 화면과 같은 순서(닫기 → 탭 → 변형)로 붙인 세 입력이 패널 상태에 따라 한 제스처를 나눠 갖는 계약 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ToppingPanelDismissInputTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var isOpen by mutableStateOf(true)
    private var dismissCount = 0
    private var hitCount = 0
    private var missCount = 0
    private var transformCount = 0

    private val target = ToppingHitTarget(
        centerXPx = 0f,
        centerYPx = 0f,
        imageWidthPx = TARGET_SIZE_PX,
        imageHeightPx = TARGET_SIZE_PX,
        rotationDegrees = 0f,
        borderWidthPx = 0f,
        outline = null,
    )

    @Test
    fun panelOpen_tap_dismissesAndIsNotATap() {
        setLayer()

        composeTestRule.onRoot().performTouchInput { click(center) }

        composeTestRule.runOnIdle {
            assertEquals(1, dismissCount)
            assertEquals(0, hitCount)
            assertEquals(0, missCount)
        }
    }

    @Test
    fun panelOpen_dragAfterDismiss_doesNotTransform() {
        setLayer()

        composeTestRule.onRoot().performTouchInput { down(center) }
        // isOpen == false 로 리컴포지션이 끝난다
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput {
            moveBy(Offset(120f, 0f))
            up()
        }

        composeTestRule.runOnIdle {
            assertEquals(false, isOpen)
            assertEquals(1, dismissCount)
            assertEquals(0, transformCount)
        }
    }

    @Test
    fun panelOpen_secondFingerAfterDismiss_doesNotTransform() {
        setLayer()

        composeTestRule.onRoot().performTouchInput { down(0, center) }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput {
            down(1, center + Offset(200f, 0f))
            moveBy(0, Offset(-60f, 0f))
            moveBy(1, Offset(60f, 0f))
            up(0)
            up(1)
        }

        composeTestRule.runOnIdle {
            assertEquals(false, isOpen)
            assertEquals(1, dismissCount)
            assertEquals(0, transformCount)
        }
    }

    @Test
    fun panelClosed_gestureWorksAsBefore() {
        isOpen = false
        setLayer()

        composeTestRule.onRoot().performTouchInput {
            down(center)
            moveBy(Offset(120f, 0f))
            up()
        }

        composeTestRule.runOnIdle {
            assertEquals(0, dismissCount)
            assertTrue(transformCount > 0)
        }
    }

    @Test
    fun afterDismissGestureEnds_nextGestureIsNormal() {
        setLayer()

        // 닫힌다
        composeTestRule.onRoot().performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        // 이번엔 탭이다
        composeTestRule.onRoot().performTouchInput { click(center) }

        composeTestRule.runOnIdle {
            assertEquals(1, dismissCount)
            assertEquals(1, hitCount)
        }
    }

    private fun setLayer() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .dismissPanelOnTouch(
                        isPanelOpen = { isOpen },
                        onDismiss = {
                            dismissCount++
                            isOpen = false
                        },
                    ).toppingTapInput(
                        entries = { listOf(Unit to target) },
                        keyOf = { it },
                        onHit = { hitCount++ },
                        onMiss = { missCount++ },
                        enabled = { !isOpen },
                    ).toppingTransformInput(
                        targetAt = { target },
                        onTransform = { _, _, _ -> transformCount++ },
                        enabled = { !isOpen },
                    ),
            )
        }
    }
}
