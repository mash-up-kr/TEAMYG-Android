package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingLottie
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingTone
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

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
        indicator = { GroupListRefreshIndicator(isRefreshing = isRefreshing, state = state) },
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

/**
 * 콘텐츠가 내려가며 생긴 위쪽 빈자리에 로띠를 놓는다. 머티리얼 기본 인디케이터의 원형 컨테이너를
 * 쓰지 않는 이유는 그 안에 로띠를 넣으면 디자인에 없는 흰 원이 하나 더 생겨서다.
 *
 * 리스트가 밝은 바탕이라 [YGLoadingTone.Dark] 다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.GroupListRefreshIndicator(
    isRefreshing: Boolean,
    state: PullToRefreshState,
) {
    val pulledFraction = { state.distanceFraction.coerceIn(0f, 1f) }

    YGLoadingLottie(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .size(INDICATOR_SIZE)
            .graphicsLayer {
                alpha = pulledFraction()

                // 콘텐츠가 비워 준 자리 한가운데. 덜 당겼으면 음수라 위로 잘려, 열린 틈만큼만 보인다
                val gap = state.distanceFraction * PullToRefreshDefaults.PositionalThreshold.toPx()
                translationY = (gap - INDICATOR_SIZE.toPx()) / 2f
            },
        tone = YGLoadingTone.Dark,
        progress = if (isRefreshing) null else pulledFraction,
    )
}

private val INDICATOR_SIZE = SizeTokens.Size44.getDp()
