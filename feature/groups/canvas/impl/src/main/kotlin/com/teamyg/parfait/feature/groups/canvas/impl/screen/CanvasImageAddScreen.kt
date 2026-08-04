package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvas
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuAction
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuItem
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChip
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChipStyle
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

private const val MAX_VISIBLE_MEMBER_CHIPS = 5

data class GroupMemberChip(
    val nicknameInitial: String,
    val colorChipType: YGColorChipType,
)

@Composable
internal fun CanvasImageAddScreen(
    groupName: String,
    memberChips: List<GroupMemberChip>,
    canvasDate: String,
    canvasDay: String,
    onClickBack: () -> Unit,
    onClickDateSelect: () -> Unit,
    onClickMenu: () -> Unit,
    onClickCamera: () -> Unit,
    onClickGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val openMenu = { isMenuExpanded = true }

    Column(modifier = modifier.background(YGAtomicColors.Gray.White)) {
        CanvasImageAddTopBar(
            groupName = groupName,
            memberChips = memberChips,
            onClickBack = onClickBack,
            onClickMenu = onClickMenu,
        )

        YGCanvas(
            date = canvasDate,
            day = canvasDay,
            onDateSelectClick = onClickDateSelect,
            addAction = YGCanvasMenuAction(
                text = "토핑 추가",
                iconResource = DesignSystemR.drawable.ic_plus,
                onClick = openMenu,
            ),
            editAction = YGCanvasMenuAction(
                text = "캔버스 편집",
                iconResource = DesignSystemR.drawable.ic_caret_right,
                onClick = openMenu,
            ),
            isEmpty = true,
            isDimmed = isMenuExpanded,
            onDimClick = { isMenuExpanded = false },
            isMenuExpanded = isMenuExpanded,
            expandedItems = listOf(
                YGCanvasMenuItem(
                    text = "카메라로 촬영",
                    onClick = {
                        isMenuExpanded = false
                        onClickCamera()
                    },
                ),
                YGCanvasMenuItem(
                    text = "갤러리에서 선택",
                    onClick = {
                        isMenuExpanded = false
                        onClickGallery()
                    },
                ),
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun CanvasImageAddTopBar(
    groupName: String,
    memberChips: List<GroupMemberChip>,
    onClickBack: () -> Unit,
    onClickMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = YGTheme.layout.padding.padding3,
                start = 18.dp, // 공통에 없음
                end = 18.dp, // 공통에 없음
                bottom = YGTheme.layout.padding.padding6,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        YGIconButton(
            iconResource = DesignSystemR.drawable.ic_caret_left,
            size = YGIconButtonSize.SIZE_44,
            contentDescription = null,
            onClick = onClickBack,
        )

        Text(
            text = groupName,
            style = YGTheme.typography.body.b01R,
            color = YGAtomicColors.Gray.Gray800,
            modifier = Modifier.weight(1f),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(-12.dp)) {
            memberChips.take(MAX_VISIBLE_MEMBER_CHIPS).forEach { member ->
                YGNametagChip(
                    colorChipType = member.colorChipType,
                    userFirstName = member.nicknameInitial,
                    chip = YGNametagChipStyle.Style28,
                )
            }

            val overflowCount = memberChips.size - MAX_VISIBLE_MEMBER_CHIPS
            if (overflowCount > 0) {
                YGNametagChip(
                    colorChipType = YGColorChipType.NametagChipPlus,
                    userFirstName = "+$overflowCount",
                    chip = YGNametagChipStyle.Style28,
                )
            }
        }

        YGIconButton(
            iconResource = DesignSystemR.drawable.ic_hamburger,
            size = YGIconButtonSize.SIZE_44,
            contentDescription = null,
            onClick = onClickMenu,
        )
    }
}

@YGPreview
@Composable
private fun PreviewCanvasImageAddScreen() = PreviewBox {
    CanvasImageAddScreen(
        groupName = "그룹이름은최대열글자",
        memberChips = listOf(
            GroupMemberChip("문", YGColorChipType.NametagChip1),
            GroupMemberChip("전", YGColorChipType.NametagChip8),
            GroupMemberChip("김", YGColorChipType.NametagChip5),
            GroupMemberChip("장", YGColorChipType.NametagChip3),
            GroupMemberChip("김", YGColorChipType.NametagChip11),
            GroupMemberChip("류", YGColorChipType.NametagChip6),
            GroupMemberChip("정", YGColorChipType.NametagChip2),
        ),
        canvasDate = "May 20",
        canvasDay = "(Wed)",
        onClickBack = {},
        onClickDateSelect = {},
        onClickMenu = {},
        onClickCamera = {},
        onClickGallery = {},
        modifier = Modifier.fillMaxSize(),
    )
}
