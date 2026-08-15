package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.etc.YGHorizontalDivider
import com.teamyg.parfait.core.designsystem.component.ygstrokebutton.YGStrokeButton
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.util.android.extension.verticalScrollbar

private val DropdownWidth = 120.dp
private val DropdownMaxHeight = 220.dp

private val BorderWidth = SizeTokens.Size1.getDp()

/**
 * 항목은 [YGStrokeButton] 에서 배경·높이·눌림 색만 가져오고 테두리는 끈다. 선을 팝업이
 * 한 번만 그어야 어느 항목이 뷰포트 끝에 걸리든 두께가 [BorderWidth] 로 고정된다 —
 * 버튼마다 두르고 겹쳐 지우는 방식은 겹침이 빠지는 순간 두 배로 두꺼워진다.
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
            // 둘 다 verticalScroll 보다 앞에 둬야 스크롤이 반영되지 않은 뷰포트 좌표에 그린다
            .border(
                width = BorderWidth,
                color = YGAtomicColors.Gray.Gray500,
            ).verticalScrollbar(
                scrollState = scrollState,
                color = YGAtomicColors.Gray.Gray400,
            ).verticalScroll(scrollState),
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
