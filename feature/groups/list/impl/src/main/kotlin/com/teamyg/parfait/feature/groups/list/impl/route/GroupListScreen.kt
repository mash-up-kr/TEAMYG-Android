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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroup
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroupType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.reveal.isStaggerRevealed
import com.teamyg.parfait.core.ui.reveal.rememberBatchReveal
import com.teamyg.parfait.core.ui.reveal.rememberStaggeredReveal
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

/** 크림이 내려오는 시간과 맞춘다 — 크림이 자리를 잡은 뒤 다음 토핑이 뜬다 */
private const val STAGGER_STEP_MILLIS = 400L

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
    onToppingsVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 미조회(null)와 0건은 그릴 토핑이 없다는 점에서 같다 —
    // 둘을 가르는 일은 툴팁 쪽(isTooltipVisible)이 맡는다
    val groupList = uiState.groupList.orEmpty()

    var settledGroupIds by remember { mutableStateOf(emptySet<GroupId>()) }

    val toppingsVisible = rememberBatchReveal(
        settled = groupList.map { it.groupId in settledGroupIds },
    )

    // 덮개는 게이트가 풀리는 순간 걷혀야 토핑이 쌓이는 모습이 보인다
    val currentOnToppingsVisibleChange by rememberUpdatedState(onToppingsVisibleChange)

    LaunchedEffect(toppingsVisible) {
        currentOnToppingsVisibleChange(toppingsVisible)
    }

    // 조회는 재진입마다 나가므로 좁히지 않으면 돌아올 때마다 파르페가 다시 쌓인다.
    // 덮개가 쓰는 기준(GroupListViewModel.isInitialLoad)에 맞춘다
    val staggerOnEntry = remember { uiState.groupList == null }

    val staggeredCount = rememberStaggeredReveal(
        total = groupList.size,
        started = toppingsVisible,
        stepMillis = STAGGER_STEP_MILLIS,
    )
    val revealedCount = if (staggerOnEntry) staggeredCount else Int.MAX_VALUE

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
                            revealedCount = revealedCount,
                            onGroupSettled = { settledGroupIds = settledGroupIds + it },
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
    onClickTopping: (GroupId) -> Unit,
    modifier: Modifier = Modifier,
    revealedCount: Int = Int.MAX_VALUE,
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
        ToppingLayout(
            contentPadding = PaddingValues(
                top = if (groupList.size <= SPECIAL_RULE_THRESHOLD) 108.dp else 96.dp,
                end = YGTheme.layout.padding.padding2,
                start = YGTheme.layout.padding.padding2,
            ),
            revealedCount = revealedCount,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 한 화면의 카드가 서로 다른 기준 시각으로 재지 않도록 목록마다 한 번만 읽는다
            val now = remember(groupList) { Clock.System.now() }

            groupList.fastForEachIndexed { index, group ->
                // 키가 없으면 목록이 바뀔 때 슬롯이 자리 기준으로 재사용된다. 결말 보고는
                // 요청당 한 번뿐이라, 그 그룹은 영영 세어지지 않고 화면이 로딩에 갇힌다
                key(group.groupId) {
                    val revealed = isStaggerRevealed(index = index, revealedCount = revealedCount)

                    YGToppingGroup(
                        image = group.toToppingImage(),
                        name = group.groupName.value,
                        timestamp = group.recentImageUploadedAt
                            .toGroupTimestamp(now)
                            .toStringResource(),
                        chipType = group.lastPlacedByNametagChip.toGrouptagChipType(),
                        type = TOPPING_PLACEMENT_TYPES[index % TOPPING_PLACEMENT_TYPES.size],
                        onImageSettled = { onGroupSettled(group.groupId) },
                        modifier = Modifier
                            .alpha(if (revealed) 1f else 0f)
                            .clickableYGScaleRipple(enabled = revealed) {
                                onClickTopping(group.groupId)
                            },
                    )
                }
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
        onToppingsVisibleChange = {},
    )
}
