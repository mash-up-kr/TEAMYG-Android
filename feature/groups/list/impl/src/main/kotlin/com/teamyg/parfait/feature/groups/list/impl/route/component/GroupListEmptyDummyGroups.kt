package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroup
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingImage
import com.teamyg.parfait.feature.groups.list.impl.R
import com.teamyg.parfait.feature.groups.list.impl.model.GROUP_LIST_EMPTY_DUMMY_GROUPS
import com.teamyg.parfait.feature.groups.list.impl.model.GroupListEmptyIntroState

private val DUMMY_ENTER_OFFSET_Y = (-60).dp

/** 직계 자식이 `ToppingLayout` 의 자리 단위라 `YGToppingGroup` 을 감싸지 않고 바로 낸다. */
@Composable
internal fun GroupListEmptyDummyGroups(intro: GroupListEmptyIntroState) {
    GROUP_LIST_EMPTY_DUMMY_GROUPS.forEachIndexed { index, dummy ->
        YGToppingGroup(
            image = YGToppingImage.Local(dummy.imageRes),
            name = stringResource(dummy.nameRes),
            timestamp = stringResource(R.string.group_list_timestamp_minutes, dummy.minutesAgo),
            chipType = dummy.chipType,
            type = dummy.type,
            modifier = Modifier
                .graphicsLayer {
                    val progress = intro.dummyProgress(index)
                    translationY = (1f - progress) * DUMMY_ENTER_OFFSET_Y.toPx()
                    alpha = progress * intro.exitAlpha
                }.clearAndSetSemantics { },
        )
    }
}
