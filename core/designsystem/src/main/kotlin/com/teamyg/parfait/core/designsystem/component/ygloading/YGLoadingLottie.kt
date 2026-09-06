package com.teamyg.parfait.core.designsystem.component.ygloading

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.teamyg.parfait.core.designsystem.R

const val YG_LOADING_LOTTIE_TEST_TAG = "yg_loading_lottie"

/**
 * 로딩 애니메이션 애셋. 색만 다른 것과 그림 자체가 다른 것을 함께 고른다.
 */
enum class YGLoadingArt(
    @get:RawRes internal val rawRes: Int,
    internal val intrinsicSize: DpSize,
) {
    /** 어두운 바탕 위에 쓴다 */
    Light(R.raw.loading_light, DpSize(44.dp, 44.dp)),

    /** 밝은 바탕 위에 쓴다 */
    Dark(R.raw.loading_dark, DpSize(44.dp, 44.dp)),

    Topping(R.raw.loading_topping, DpSize(90.dp, 106.dp)),
}

/**
 * 로딩 애니메이션. 애셋과 재생 방식을 한 곳에 두어, 쓰는 쪽이 `R.raw` 를 직접 가리키지 않게 한다.
 *
 * @param progress 그릴 지점을 직접 정한다. 당겨서 새로고침처럼 **손가락을 따라가야 할 때** 쓴다.
 *   넘기지 않으면 스스로 무한 반복한다 — 로딩은 끝나는 시점을 스스로 모르기 때문이다.
 * @param modifier 비워 두면 애셋 원본 크기로 그린다 — 그 크기에서 다시 그리는 일이 없다
 */
@Composable
fun YGLoadingLottie(
    modifier: Modifier = Modifier,
    art: YGLoadingArt = YGLoadingArt.Light,
    progress: (() -> Float)? = null,
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(art.rawRes),
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
        modifier = Modifier
            .size(art.intrinsicSize)
            .then(modifier)
            .testTag(YG_LOADING_LOTTIE_TEST_TAG),
    )
}
