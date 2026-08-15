package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroup
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroupType
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingImage
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingTemplate
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.feature.groups.list.impl.R
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListParfaitLayout
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListPullToRefreshBox
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListTopBar
import com.teamyg.parfait.feature.groups.list.impl.route.component.ToppingLayout
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

private const val SPECIAL_RULE_THRESHOLD = 3

// Todo : 로직 추후 변경하기
private val TOPPING_PLACEMENT_TYPES = listOf(
    YGToppingGroupType.TYPE_1_LEFT,
    YGToppingGroupType.TYPE_1_RIGHT,
    YGToppingGroupType.TYPE_2_LEFT,
    YGToppingGroupType.TYPE_2_RIGHT,
    YGToppingGroupType.TYPE_3_LEFT,
    YGToppingGroupType.TYPE_3_RIGHT,
)

// TODO(칩 컬러): 칩 타입은 마지막으로 그룹을 바꾼 유저의 Nametag-Chip 타입을 따라야 한다.
//  MyParfaitGroupVO 에 그 정보가 오면 index 순환을 걷어낸다
private val CHIP_TYPES = YGGrouptagChipType.entries

// TODO(토핑 템플릿): 정책은 그룹 생성 시 6종 중 하나를 무작위로 골라 고정하는 것이다.
//  서버가 그 값을 내려주면 groupId 파생을 걷어낸다
private val TOPPING_TEMPLATES = YGToppingTemplate.entries

/**
 * 아직 토핑이 없는 그룹은 조회 실패([YGToppingImage.Error])와 다른 상태라 템플릿을 띄운다.
 * 목록 순서가 바뀌어도 같은 그림이 걸리도록 index 가 아니라 groupId 로 고른다.
 */
internal fun MyParfaitGroupVO.toToppingImage(): YGToppingImage = recentImageUrl
    ?.let(YGToppingImage::Remote)
    ?: YGToppingImage.Template(TOPPING_TEMPLATES[groupId.value.mod(TOPPING_TEMPLATES.size)])

@Composable
internal fun GroupListScreen(
    uiState: GroupListUiState,
    onClickChip: () -> Unit,
    onClickSideMenu: () -> Unit,
    onClickTopping: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column {
            GroupListTopBar(
                date = uiState.dateString,
                day = uiState.dayOfWeekString,
                onClickSideMenu = onClickSideMenu,
                onClickAddGroup = onClickChip,
            )

            GroupListPullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = YGTheme.layout.padding.padding7,
                        top = YGTheme.layout.padding.padding10,
                        end = YGTheme.layout.padding.padding7,
                        bottom = YGTheme.layout.padding.padding6,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item {
                        GroupListContent(
                            groupList = uiState.groupList,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun GroupListContent(
    groupList: List<MyParfaitGroupVO>,
    modifier: Modifier = Modifier,
) {
    GroupListParfaitLayout(
        cherrySection = {
            Image(
                painter = painterResource(R.drawable.parfait_cherry),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(83.dp),
            )
        },
        topSection = {
            Image(
                painter = painterResource(R.drawable.parfait_cream_top),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(210.dp),
            )
        },
        middleSection = {
            Image(
                painter = painterResource(R.drawable.parfait_cream_default),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(241.dp),
            )
        },
        bottomSection = {
            Image(
                painter = painterResource(R.drawable.parfait_cup),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(324.dp),
            )
        },
        modifier = modifier,
    ) {
        ToppingLayout(
            contentPadding = PaddingValues(
                top = if (groupList.size <= SPECIAL_RULE_THRESHOLD) 108.dp else 96.dp,
                end = YGTheme.layout.padding.padding2,
                start = YGTheme.layout.padding.padding2,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 한 화면의 카드가 서로 다른 기준 시각으로 재지 않도록 목록마다 한 번만 읽는다
            val now = remember(groupList) { Clock.System.now() }
            val timeZone = TimeZone.currentSystemDefault()

            groupList.fastForEachIndexed { index, group ->
                YGToppingGroup(
                    image = group.toToppingImage(),
                    name = group.groupName.value,
                    timestamp = group.recentImageUploadedAt
                        .toGroupTimestamp(now = now, timeZone = timeZone)
                        .toStringResource(),
                    chipType = CHIP_TYPES[index % CHIP_TYPES.size],
                    type = TOPPING_PLACEMENT_TYPES[index % TOPPING_PLACEMENT_TYPES.size],
                )
            }
        }
    }
}

private class GroupListScreenPreviewParameterProvider :
    PreviewParameterProvider<GroupListUiState> {
    private val groupList = listOf(
        MyParfaitGroupVO(
            groupId = GroupId(1L),
            groupName = GroupName("매시업"),
            recentImageUrl = "https://picsum.photos/id/1025/200",
            recentImageUploadedAt = LocalDateTime(2026, 8, 15, 9, 57),
        ),
        MyParfaitGroupVO(
            groupId = GroupId(2L),
            groupName = GroupName("매시업매시업매시업"),
            recentImageUrl = "https://picsum.photos/id/1062/200",
            recentImageUploadedAt = LocalDateTime(2026, 8, 15, 8, 0),
        ),
        MyParfaitGroupVO(
            groupId = GroupId(3L),
            groupName = GroupName("우리집"),
            recentImageUrl = null,
            recentImageUploadedAt = null,
        ),
    )

    override val values: Sequence<GroupListUiState>
        get() = sequenceOf(
            GroupListUiState(
                groupList = groupList,
                groupAddButtonSelected = false,
                isTooltipVisible = false,
                dateString = "July 26",
                dayOfWeekString = "Wed",
            ),
            GroupListUiState(
                groupList = groupList,
                groupAddButtonSelected = true,
                isTooltipVisible = false,
                dateString = "July 26",
                dayOfWeekString = "Wed",
            ),
            GroupListUiState(
                groupList = emptyList(),
                groupAddButtonSelected = false,
                isTooltipVisible = true,
                dateString = "July 26",
                dayOfWeekString = "Wed",
            ),
        )
}

@YGPreview
@Composable
private fun GroupListScreenPreview(
    @PreviewParameter(GroupListScreenPreviewParameterProvider::class) uiState: GroupListUiState,
) = PreviewBox {
    GroupListScreen(
        uiState = uiState,
        onClickChip = {},
        onClickSideMenu = {},
        onClickTopping = {},
        onRefresh = {},
    )
}
