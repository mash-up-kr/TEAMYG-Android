package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingLottie
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingTone
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroup
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroupType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGScaleRipple
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.feature.groups.list.impl.R
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListParfaitLayout
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListPullToRefreshBox
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListTopBar
import com.teamyg.parfait.feature.groups.list.impl.route.component.ToppingLayout
import com.teamyg.parfait.feature.groups.list.impl.util.toGroupTimestamp
import com.teamyg.parfait.feature.groups.list.impl.util.toGrouptagChipType
import com.teamyg.parfait.feature.groups.list.impl.util.toStringResource
import com.teamyg.parfait.feature.groups.list.impl.util.toToppingImage
import kotlin.time.Clock
import kotlin.time.Instant

private const val SPECIAL_RULE_THRESHOLD = 3

// 공통 로딩 정책이 정한 애셋 원본 크기다 — 그 크기에서 다시 그리는 일이 없다
private val LOADING_SIDE = 44.dp

// Todo : 로직 추후 변경하기
private val TOPPING_PLACEMENT_TYPES = listOf(
    YGToppingGroupType.TYPE_1_LEFT,
    YGToppingGroupType.TYPE_1_RIGHT,
    YGToppingGroupType.TYPE_2_LEFT,
    YGToppingGroupType.TYPE_2_RIGHT,
    YGToppingGroupType.TYPE_3_LEFT,
    YGToppingGroupType.TYPE_3_RIGHT,
)

@Composable
internal fun GroupListScreen(
    uiState: GroupListUiState,
    onClickChip: () -> Unit,
    onClickSideMenu: () -> Unit,
    onClickTopping: (GroupId) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 미조회(null)와 0건은 그릴 토핑이 없다는 점에서 같다 —
    // 둘을 가르는 일은 툴팁 쪽(isTooltipVisible)이 맡는다
    val groupList = uiState.groupList.orEmpty()

    // 토핑이 하나씩 따로 뜨는 대신, 전부 결말날 때까지 가렸다가 한 번에 드러낸다.
    //
    // 한 번 드러낸 뒤에는 다시 가리지 않는다 — 그룹을 새로 만들 때마다 목록 전체가
    // 사라졌다 나타나면 더 거슬린다. 뒤늦게 들어온 그룹은 저 혼자 떠오른다.
    var settledGroupIds by remember { mutableStateOf(emptySet<GroupId>()) }
    var revealed by remember { mutableStateOf(false) }

    // 목록이 비어 있는 동안을 완료로 세면 안 된다 — 조회가 오기도 전에 빗장이 풀린다
    val allSettled = groupList.isNotEmpty() && groupList.all { it.groupId in settledGroupIds }

    LaunchedEffect(allSettled) {
        if (allSettled) revealed = true
    }

    val toppingsVisible = revealed || groupList.isEmpty()

    Box(modifier = modifier) {
        Column {
            GroupListTopBar(
                date = uiState.dateString,
                day = uiState.dayOfWeekString,
                onClickSideMenu = onClickSideMenu,
                onClickAddGroup = onClickChip,
                isTooltipVisible = uiState.isTooltipVisible,
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
                            groupList = groupList,
                            onClickTopping = onClickTopping,
                            toppingsVisible = toppingsVisible,
                            onGroupSettled = { settledGroupIds = settledGroupIds + it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        if (!toppingsVisible) {
            YGLoadingLottie(
                // Tone 은 화면 테마가 아니라 얹히는 바탕을 보고 고른다. 목록 바탕이 밝다
                tone = YGLoadingTone.Dark,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(LOADING_SIDE),
            )
        }
    }
}

@Composable
internal fun GroupListContent(
    groupList: List<MyParfaitGroupVO>,
    onClickTopping: (GroupId) -> Unit,
    modifier: Modifier = Modifier,
    toppingsVisible: Boolean = true,
    onGroupSettled: (GroupId) -> Unit = {},
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
        // 그리지 않고 감추면 안 된다. 감춘 자리도 배치는 살아 있어야 이미지 요청이 이어진다
        ToppingLayout(
            contentPadding = PaddingValues(
                top = if (groupList.size <= SPECIAL_RULE_THRESHOLD) 108.dp else 96.dp,
                end = YGTheme.layout.padding.padding2,
                start = YGTheme.layout.padding.padding2,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (toppingsVisible) 1f else 0f),
        ) {
            // 한 화면의 카드가 서로 다른 기준 시각으로 재지 않도록 목록마다 한 번만 읽는다
            val now = remember(groupList) { Clock.System.now() }

            groupList.fastForEachIndexed { index, group ->
                YGToppingGroup(
                    image = group.toToppingImage(),
                    name = group.groupName.value,
                    timestamp = group.recentImageUploadedAt
                        .toGroupTimestamp(now)
                        .toStringResource(),
                    chipType = group.lastPlacedByNametagChip.toGrouptagChipType(),
                    type = TOPPING_PLACEMENT_TYPES[index % TOPPING_PLACEMENT_TYPES.size],
                    onImageSettled = { onGroupSettled(group.groupId) },
                    // 드러나기 전에는 누를 수 없다 — 보이지 않는 토핑이 눌리면 안 된다
                    modifier = if (toppingsVisible) {
                        Modifier.clickableYGScaleRipple { onClickTopping(group.groupId) }
                    } else {
                        Modifier
                    },
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
            recentImageUploadedAt = Instant.parse("2026-08-15T09:57:00Z"),
            lastPlacedByNametagChip = NametagChipType.TYPE1,
        ),
        MyParfaitGroupVO(
            groupId = GroupId(2L),
            groupName = GroupName("매시업매시업매시업"),
            recentImageUrl = "https://picsum.photos/id/1062/200",
            recentImageUploadedAt = Instant.parse("2026-08-15T08:00:00Z"),
            lastPlacedByNametagChip = NametagChipType.TYPE9,
        ),
        MyParfaitGroupVO(
            groupId = GroupId(3L),
            groupName = GroupName("우리집"),
            recentImageUrl = null,
            recentImageUploadedAt = null,
            lastPlacedByNametagChip = NametagChipType.DEFAULT,
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
