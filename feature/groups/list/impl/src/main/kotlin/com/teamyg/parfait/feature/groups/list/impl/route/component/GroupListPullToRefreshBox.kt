package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.lerp
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingLottie
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingTone
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.feature.groups.list.impl.R
import kotlin.math.max

/** 당기는 동안 인디케이터만 내려오는 기본 동작과 달리, 콘텐츠도 당긴 거리만큼 함께 내려간다 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupListPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    var indicatorHeight by remember { mutableFloatStateOf(0f) }
    val messageAlpha = animateFloatAsState(
        targetValue = if (isRefreshing) 1f else 0f,
        label = "GroupListRefreshMessageAlpha",
    )

    // 임계값이 벌리는 틈에는 로띠밖에 들어가지 않아, 문구는 새로고침이 확정된 뒤 더 벌려서 놓는다
    val gap = {
        with(density) {
            max(
                state.distanceFraction * PullToRefreshDefaults.PositionalThreshold.toPx(),
                (indicatorHeight + INDICATOR_VERTICAL_SPACE.toPx()) * messageAlpha.value,
            )
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {
            GroupListRefreshIndicator(
                isRefreshing = isRefreshing,
                state = state,
                gap = gap,
                messageAlpha = messageAlpha,
                onHeightChange = { indicatorHeight = it },
            )
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = gap() },
        ) {
            content()
        }
    }
}

/**
 * 머티리얼 기본 인디케이터의 원형 컨테이너를 쓰지 않는 이유는 그 안에 로띠를 넣으면 디자인에
 * 없는 흰 원이 하나 더 생겨서다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.GroupListRefreshIndicator(
    isRefreshing: Boolean,
    state: PullToRefreshState,
    gap: () -> Float,
    messageAlpha: State<Float>,
    onHeightChange: (Float) -> Unit,
) {
    val pulledFraction = { state.distanceFraction.coerceIn(0f, 1f) }

    // 알파만 0으로 두면 TalkBack 이 안 보이는 문구를 읽고, 사라지는 중에 빼면 블록 높이가 튄다
    val isMessageVisible by remember { derivedStateOf { messageAlpha.value > 0f } }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .onSizeChanged { onHeightChange(it.height.toFloat()) }
            .graphicsLayer {
                alpha = pulledFraction()

                // 열린 틈의 한가운데. 덜 당겼으면 음수라 위로 잘려, 열린 틈만큼만 보인다.
                // 문구가 붙는 순간 블록 높이는 한 번에 커지므로, 기준은 알파를 따라 늘려야 안 튄다
                val blockHeight = lerp(INDICATOR_SIZE.toPx(), size.height, messageAlpha.value)
                translationY = (gap() - blockHeight) / 2f
            },
    ) {
        YGLoadingLottie(
            modifier = Modifier.size(INDICATOR_SIZE),
            tone = YGLoadingTone.Dark,
            progress = if (isRefreshing) null else pulledFraction,
        )

        if (isMessageVisible) {
            Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap7))

            Text(
                text = stringResource(R.string.group_list_refreshing),
                style = YGTheme.typography.title.t03SB,
                color = YGAtomicColors.Gray.Gray500,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = messageAlpha.value },
            )
        }
    }
}

private val INDICATOR_SIZE = SizeTokens.Size44.getDp()

@OptIn(ExperimentalMaterial3Api::class)
private val INDICATOR_VERTICAL_SPACE = PullToRefreshDefaults.PositionalThreshold - INDICATOR_SIZE
