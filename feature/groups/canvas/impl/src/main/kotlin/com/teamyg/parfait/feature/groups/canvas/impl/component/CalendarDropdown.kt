package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.etc.YGHorizontalDivider
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

private val DropdownWidth = 120.dp
private val DropdownMaxHeight = 220.dp
private val ItemVerticalPadding = 11.5.dp

private val ScrollbarWidth = 2.dp
private val ScrollbarEndPadding = 3.dp
private val ScrollbarVerticalPadding = 4.dp

/** TODO(#207): 항목 좌우 여백과 모서리 반경은 아직 시안이 없어 임의값이다 */
@Composable
internal fun <T> CalendarDropdown(
    items: List<T>,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .background(YGAtomicColors.Gray.White)
            .border(
                width = SizeTokens.Size1.getDp(),
                color = YGAtomicColors.Gray.Gray500,
            ).width(DropdownWidth)
            .heightIn(max = DropdownMaxHeight)
            // verticalScroll 보다 앞에 둬야 스크롤 이동이 적용되지 않은 뷰포트 좌표에 그린다
            .verticalScrollbar(scrollState)
            .verticalScroll(scrollState),
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                YGHorizontalDivider(color = YGAtomicColors.Gray.Gray500)
            }

            Text(
                text = itemLabel(item),
                style = YGTheme.typography.body.b02R,
                color = YGAtomicColors.Gray.Gray700,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item) }
                    .padding(
                        horizontal = YGTheme.layout.padding.padding6,
                        vertical = ItemVerticalPadding,
                    ),
            )
        }
    }
}

/**
 * Compose 에는 [ScrollState] 용 스크롤바가 없어 직접 그린다. `drawBehind` 가 아니라
 * `drawWithContent` 인 이유는 막대가 항목 텍스트 위에 얹혀야 해서다.
 */
private fun Modifier.verticalScrollbar(
    scrollState: ScrollState,
    width: Dp = ScrollbarWidth,
    color: Color = YGAtomicColors.Gray.Gray400,
): Modifier = drawWithContent {
    drawContent()
    if (scrollState.maxValue <= 0) return@drawWithContent

    val viewportHeight = size.height
    val contentHeight = viewportHeight + scrollState.maxValue

    // 막대가 오르내리는 구간. 위아래 여백만큼 짧다
    val trackTop = ScrollbarVerticalPadding.toPx()
    val trackHeight = viewportHeight - trackTop * 2

    // 뷰포트가 전체 내용에서 차지하는 비율이 곧 막대 길이다
    val thumbHeight = trackHeight * viewportHeight / contentHeight
    val scrollRatio = scrollState.value.toFloat() / scrollState.maxValue
    val thumbWidth = width.toPx()

    drawRoundRect(
        color = color,
        topLeft = Offset(
            x = size.width - ScrollbarEndPadding.toPx() - thumbWidth,
            y = trackTop + scrollRatio * (trackHeight - thumbHeight),
        ),
        size = Size(width = thumbWidth, height = thumbHeight),
        cornerRadius = CornerRadius(thumbWidth / 2),
    )
}
