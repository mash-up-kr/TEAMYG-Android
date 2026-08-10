package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 그룹 리스트와 에러 화면이 공유하는 pull-to-refresh 컨테이너.
 *
 * 당기는 동안 인디케이터만 내려오는 기본 동작과 달리, 콘텐츠도 당긴 거리만큼 함께 내려간다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupListPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    content: @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = state.distanceFraction * PullToRefreshDefaults.PositionalThreshold.toPx()
                },
        ) {
            content()
        }
    }
}
