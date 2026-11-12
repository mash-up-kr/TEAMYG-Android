package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.PreviewParameter
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.designsystem.image.rememberReloadableImageRequest
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasLoadState

/**
 * 타임랩스 영상의 배경 한 장. 미리보기와 녹화 레이어가 함께 쓴다 — 둘이 어긋나면 미리보기와 저장된
 * 영상이 다르게 보인다. 배경을 안 고른 캔버스에도 토핑은 흰 바탕 위에 깔린다.
 *
 * @param retryKey 캔버스 메인과 같은 값. 0 이면 재시도 뒤에도 캐시에 앉은 옛 실패 바이트를 읽는다
 * @param onImageLoadStateChange 이미지 배경의 로드 결과. 녹화가 첫 프레임 시점을 정하는 데 쓴다.
 *   색 배경은 기다릴 것이 없어 부르지 않는다
 */
@Composable
internal fun CanvasVideoBackground(
    background: YGCanvasBackground?,
    retryKey: Int,
    modifier: Modifier = Modifier,
    onImageLoadStateChange: ((CanvasLoadState) -> Unit)? = null,
) {
    when (background) {
        null -> Box(modifier = modifier.background(color = YGAtomicColors.Gray.White))

        is YGCanvasBackground.Solid -> Box(modifier = modifier.background(color = background.color))

        is YGCanvasBackground.Image -> AsyncImage(
            model = rememberReloadableImageRequest(background.url, retryKey),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onState = onImageLoadStateChange?.let { onChange ->
                { state -> onChange(state.toCanvasLoadState()) }
            },
            modifier = modifier,
        )
    }
}

private fun AsyncImagePainter.State.toCanvasLoadState(): CanvasLoadState = when (this) {
    is AsyncImagePainter.State.Success -> CanvasLoadState.Loaded
    is AsyncImagePainter.State.Error -> CanvasLoadState.Failed
    else -> CanvasLoadState.Loading
}

private class CanvasVideoBackgroundPreviewParameterProvider :
    NamedPreviewParameterProvider<YGCanvasBackground?>() {
    override val entries: List<Pair<String, YGCanvasBackground?>> = listOf(
        "none" to null,
        "solid" to YGCanvasBackground.Solid(YGAtomicColors.Gray.Black),
    )
}

@YGPreview
@Composable
private fun CanvasVideoBackgroundPreview(
    @PreviewParameter(CanvasVideoBackgroundPreviewParameterProvider::class) background: YGCanvasBackground?,
) = PreviewBox {
    CanvasVideoBackground(
        background = background,
        retryKey = 0,
        modifier = Modifier.fillMaxSize(),
    )
}
