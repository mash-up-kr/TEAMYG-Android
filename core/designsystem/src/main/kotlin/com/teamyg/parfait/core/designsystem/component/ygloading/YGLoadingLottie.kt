package com.teamyg.parfait.core.designsystem.component.ygloading

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.teamyg.parfait.core.designsystem.R

/**
 * 로딩 애니메이션의 색. 화면 테마가 아니라 **애니메이션이 얹히는 바탕**을 보고 고른다 —
 * 로딩 오버레이는 Dim 위라 [Light], 그룹 리스트처럼 밝은 바탕 위라면 [Dark] 다.
 */
enum class YGLoadingTone(
    @get:RawRes internal val rawRes: Int,
) {
    /** 밝은 색 애니메이션. 어두운 바탕 위에 쓴다 */
    Light(R.raw.loading_light),

    /** 어두운 색 애니메이션. 밝은 바탕 위에 쓴다 */
    Dark(R.raw.loading_dark),
}

/**
 * 로딩 애니메이션. 애셋과 재생 방식을 한 곳에 두어, 쓰는 쪽이 `R.raw` 를 직접 가리키지 않게 한다.
 *
 * @param tone 얹히는 바탕에 맞춰 고른다. 기준은 [YGLoadingTone] 에 있다
 * @param progress 그릴 지점을 직접 정한다. 당겨서 새로고침처럼 **손가락을 따라가야 할 때** 쓴다.
 *   넘기지 않으면 스스로 무한 반복한다 — 로딩은 끝나는 시점을 스스로 모르기 때문이다.
 * @param modifier 크기를 묶어 두는 것은 쓰는 쪽 몫이다. 비워 두면 놓인 자리만큼 늘어난다
 */
@Composable
fun YGLoadingLottie(
    modifier: Modifier = Modifier,
    tone: YGLoadingTone = YGLoadingTone.Light,
    progress: (() -> Float)? = null,
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(tone.rawRes),
    )
    val loopingProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        // 쓰는 쪽이 진행을 잡고 있으면 시계를 두 개 돌리지 않는다
        isPlaying = progress == null,
    )

    LottieAnimation(
        composition = composition,
        progress = progress ?: { loopingProgress },
        modifier = modifier,
    )
}
