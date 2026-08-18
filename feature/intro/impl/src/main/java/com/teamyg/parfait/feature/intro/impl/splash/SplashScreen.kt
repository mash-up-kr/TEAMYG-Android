package com.teamyg.parfait.feature.intro.impl.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.intro.impl.R
import kotlinx.coroutines.flow.first

/**
 * @param onAnimationFinished 로띠가 끝났음을 알린다. 다음 화면으로 갈지는 화면이 정하지 않는다 —
 *   서버 응답도 함께 기다려야 하고 그 판단은 ViewModel 에 있다
 */
@Composable
internal fun SplashScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compositionResult = rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.splash),
    )
    val composition = compositionResult.value
    val progress by animateLottieCompositionAsState(
        composition = composition,
        // 한 번만 재생한다. 반복하면 끝나는 시점이 없어 다음 화면으로 갈 신호를 만들 수 없다
        iterations = 1,
    )
    // 아래 이펙트는 한 번 서서 끝까지 산다 — 그 사이 다시 넘어온 콜백을 잡아 두는 건 여기다
    val currentOnAnimationFinished by rememberUpdatedState(onAnimationFinished)

    /*
     * 파싱 실패도 '끝'으로 넘긴다 — 로띠 하나 때문에 스플래시에 갇히는 것이 애니메이션을
     * 못 보는 것보다 나쁘다. 이 신호가 없으면 화면을 벗어날 방법이 아예 없다.
     *
     * `progress` 는 프레임마다 바뀌므로 키로 두면 재생 내내 코루틴이 매 프레임 다시 선다.
     * 키는 고정하고 값 변화는 snapshotFlow 로 흘려보내 '끝'이 되는 첫 순간만 기다린다.
     *
     * 중복 호출은 막지 않는다. 여기서 세는 대신 ViewModel 이 첫 신호만 받아들인다 —
     * 컴포지션이 다시 서는 경우까지 화면이 기억하고 있을 이유가 없다.
     */
    LaunchedEffect(Unit) {
        snapshotFlow {
            val isPlayedToEnd = compositionResult.value != null && progress >= 1f
            isPlayedToEnd || compositionResult.isFailure
        }.first { isFinished -> isFinished }

        currentOnAnimationFinished()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
        )
    }
}

@YGPreview
@Composable
private fun SplashScreenPreview() = PreviewBox {
    SplashScreen(
        onAnimationFinished = {},
        modifier = Modifier.fillMaxSize(),
    )
}
