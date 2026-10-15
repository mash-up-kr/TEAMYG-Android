package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val VIEWPORT_TAG = "arrange_viewport"
private const val CANVAS_TAG = "arrange_canvas"
private const val PANEL_TAG = "arrange_panel"

/** 폭 기준 캔버스 높이가 남는 높이보다 큰 화면 */
private val SHORT_VIEWPORT = DpSize(411.dp, 731.dp)

/** 폭 기준 캔버스 높이가 남는 높이에 들어가는 화면 */
private val TALL_VIEWPORT = DpSize(360.dp, 900.dp)

private const val PIXEL_TOLERANCE = 2f

@MediumTest
@RunWith(AndroidJUnit4::class)
class ToppingArrangeLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var canvasHorizontalPaddingPx = 0f

    @Test
    fun shortViewport_canvasFitsAboveConfirmButton_andKeepsAspectRatio() {
        setLayout(SHORT_VIEWPORT)

        val viewport = boundsOf(VIEWPORT_TAG)
        val canvas = boundsOf(CANVAS_TAG)
        val panel = boundsOf(PANEL_TAG)
        val confirmTop = confirmButtonBounds().top

        assertTrue(
            "패널 아래 끝(${panel.bottom})이 확정 버튼 위 끝($confirmTop)보다 아래다",
            panel.bottom <= confirmTop + PIXEL_TOLERANCE,
        )
        assertEquals(canvas.height * CANVAS_AREA_ASPECT_RATIO, canvas.width, PIXEL_TOLERANCE)
        // 높이에 맞춰 좁아진 캔버스는 가운데에 온다
        assertTrue(canvas.width < viewport.width - canvasHorizontalPaddingPx * 2 - PIXEL_TOLERANCE)
        assertEquals(canvas.left - viewport.left, viewport.right - canvas.right, PIXEL_TOLERANCE)
    }

    @Test
    fun tallViewport_canvasFillsAvailableWidth() {
        setLayout(TALL_VIEWPORT)

        val viewport = boundsOf(VIEWPORT_TAG)
        val canvas = boundsOf(CANVAS_TAG)
        val panel = boundsOf(PANEL_TAG)

        assertEquals(viewport.width - canvasHorizontalPaddingPx * 2, canvas.width, PIXEL_TOLERANCE)
        assertEquals(canvas.height * CANVAS_AREA_ASPECT_RATIO, canvas.width, PIXEL_TOLERANCE)
        assertTrue(panel.bottom <= confirmButtonBounds().top + PIXEL_TOLERANCE)
    }

    /** `boundsInRoot` 는 잘린 뒤의 값이다. 넘침을 재려면 자르기 전 위치와 크기가 필요하다 */
    private fun boundsOf(tag: String): Rect = composeTestRule
        .onNode(hasTestTag(tag))
        .fetchSemanticsNode()
        .let { node -> Rect(node.positionInRoot, node.size.toSize()) }

    private fun confirmButtonBounds(): Rect = composeTestRule
        .onNode(hasText("캔버스에 쌓기") and hasClickAction())
        .fetchSemanticsNode()
        .let { node -> Rect(node.positionInRoot, node.size.toSize()) }

    private fun setLayout(viewport: DpSize) {
        composeTestRule.setContent {
            YGCustomTheme {
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // 기기 화면이 viewport 보다 작은 변이 있으면 밀도를 낮춰 통째로 들인다
                    val base = LocalDensity.current
                    val fit = minOf(maxWidth / viewport.width, maxHeight / viewport.height, 1f)
                    CompositionLocalProvider(
                        LocalDensity provides Density(base.density * fit, base.fontScale),
                    ) {
                        val canvasHorizontalPadding = YGTheme.layout.padding.padding7
                        canvasHorizontalPaddingPx = with(LocalDensity.current) { canvasHorizontalPadding.toPx() }
                        Box(
                            modifier = Modifier
                                .size(viewport)
                                .testTag(VIEWPORT_TAG),
                        ) {
                            ToppingArrangeLayout(
                                header = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                    )
                                },
                                onClickConfirm = {},
                                panel = {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag(PANEL_TAG),
                                    )
                                },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .testTag(CANVAS_TAG),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
