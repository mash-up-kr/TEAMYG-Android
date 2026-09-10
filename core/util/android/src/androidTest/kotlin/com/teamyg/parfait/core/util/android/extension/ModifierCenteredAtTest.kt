package com.teamyg.parfait.core.util.android.extension

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val PARENT_SIZE = DpSize(200.dp, 200.dp)
private val CHILD_SIZE = DpSize(600.dp, 400.dp)
private val CENTER = DpOffset(100.dp, 100.dp)

private const val PARENT_TAG = "parent"
private const val CONTENT_TAG = "content"

/** dp 는 픽셀로 반올림되었다가 되돌아온다 */
private const val TOLERANCE_DP = 1f

/**
 * 부모보다 큰 자식을 놓을 때의 좌표 계약. 캔버스를 넘어서게 키운 토핑이 이 경우다.
 *
 * `requiredSize` 가 부모 제약을 무시하는 것은 자기 자식을 잴 때뿐이라, 그 노드가 부모에게
 * 보고하는 크기는 잘린다. Compose 는 그 잘린 겉크기 안에 내용을 가운데 정렬한다
 * (`Placeable.apparentToRealOffset`).
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ModifierCenteredAtTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun centeredAt_childLargerThanParent_keepsContentCenterAtPoint() {
        // Given · When 부모보다 큰 자식을 centeredAt 으로 놓는다
        composeTestRule.setContent {
            Box(modifier = Modifier.requiredSize(PARENT_SIZE).testTag(PARENT_TAG)) {
                Box(
                    modifier = Modifier
                        .centeredAt(CENTER)
                        .requiredSize(CHILD_SIZE),
                ) {
                    Box(modifier = Modifier.fillMaxSize().testTag(CONTENT_TAG))
                }
            }
        }

        // Then 내용의 중심이 지정한 지점에 온다
        assertContentCenter(expectedX = CENTER.x.value, expectedY = CENTER.y.value)
    }

    @Test
    fun offsetTopLeft_childLargerThanParent_driftsByHalfOverflow() {
        // Given · When 같은 자리를 좌상단 계산으로 놓는다 — 배치 화면이 쓰던 방식이다
        composeTestRule.setContent {
            Box(modifier = Modifier.requiredSize(PARENT_SIZE).testTag(PARENT_TAG)) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = CENTER.x - CHILD_SIZE.width / 2,
                            y = CENTER.y - CHILD_SIZE.height / 2,
                        ).requiredSize(CHILD_SIZE),
                ) {
                    Box(modifier = Modifier.fillMaxSize().testTag(CONTENT_TAG))
                }
            }
        }

        // Then 넘친 양의 절반만큼 좌상단으로 밀린다 — 핸들 버튼과 벌어지는 그 거리다
        val driftX = (CHILD_SIZE.width - PARENT_SIZE.width).value / 2
        val driftY = (CHILD_SIZE.height - PARENT_SIZE.height).value / 2
        assertContentCenter(
            expectedX = CENTER.x.value - driftX,
            expectedY = CENTER.y.value - driftY,
        )
    }

    private fun assertContentCenter(
        expectedX: Float,
        expectedY: Float,
    ) {
        val parent = composeTestRule.onNodeWithTag(PARENT_TAG).getUnclippedBoundsInRoot()
        val content = composeTestRule.onNodeWithTag(CONTENT_TAG).getUnclippedBoundsInRoot()

        val centerX = (content.left + content.width / 2 - parent.left).value
        val centerY = (content.top + content.height / 2 - parent.top).value

        assertEquals(expectedX, centerX, TOLERANCE_DP)
        assertEquals(expectedY, centerY, TOLERANCE_DP)
    }
}
