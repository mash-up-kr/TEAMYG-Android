package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.teamyg.parfait.core.designsystem.component.ygstrokebutton.YGStrokeButton
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

private val DropdownWidth = 120.dp
private val DropdownMaxHeight = 220.dp

/** 팝업 테두리와 항목 사이 구분선의 공통 두께 */
private val BorderWidth = SizeTokens.Size1.getDp()

private val ScrollbarWidth = 2.dp
private val ScrollbarEndPadding = 3.dp
private val ScrollbarVerticalPadding = 4.dp

/**
 * 항목은 [YGStrokeButton] 을 쓰되 테두리는 끈다. 배경·높이·눌림 색만 버튼에서 가져오고,
 * 테두리는 팝업이 바깥에 한 번만 두른다.
 *
 * 선을 한 곳에서만 그리므로 어느 항목이 뷰포트 끝에 걸리든 두께가 [BorderWidth] 로 고정된다.
 * 버튼마다 테두리를 두르고 겹쳐서 지우는 방식은 겹침이 빠지는 순간 두 배로 두꺼워진다.
 */
@Composable
internal fun <T> CalendarDropdown(
    items: List<T>,
    selectedItem: T?,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .width(DropdownWidth)
            .heightIn(max = DropdownMaxHeight)
            // 둘 다 verticalScroll 보다 앞에 둬야 스크롤 이동이 적용되지 않은 뷰포트 좌표에 그린다.
            // border 는 내용 위에 그려지므로 끝까지 스크롤해도 항목에 가려지지 않는다
            .border(
                width = BorderWidth,
                color = YGAtomicColors.Gray.Gray500,
            ).verticalScrollbar(scrollState)
            .verticalScroll(scrollState),
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                YGHorizontalDivider(
                    thickness = BorderWidth,
                    color = YGAtomicColors.Gray.Gray500,
                )
            }

            YGStrokeButton(
                text = itemLabel(item),
                onClick = { onSelect(item) },
                isSelected = item == selectedItem,
                borderWidth = Dp.Hairline,
                modifier = Modifier.fillMaxWidth(),
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
