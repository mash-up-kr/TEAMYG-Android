package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

/**
 * 토핑 배치 화면의 캔버스 바닥. 배경을 깔고 [content] 를 캔버스 경계에서 자른다.
 *
 * @param backgroundColor [backgroundImageUrl] 이 없을 때만 그린다.
 * @param modifier 크기는 호출부가 정한다. 여기 단 것은 자르기보다 앞에 온다.
 */
@Composable
internal fun ToppingArrangeCanvasSurface(
    backgroundColor: Color,
    backgroundImageUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clipToBounds()
            .let { if (backgroundImageUrl == null) it.background(backgroundColor) else it }
            .border(
                width = 1.dp,
                color = YGAtomicColors.Gray.Gray500,
            ),
    ) {
        backgroundImageUrl?.let { imageUrl ->
            Image(
                painter = rememberAsyncImagePainter(model = imageUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        content()
    }
}

/**
 * 이보다 먼저 그린 층을 어둡게 눌러, 뒤에 그리는 편집 대상만 도드라져 보이게 한다. 어느 층
 * 사이에 둘지는 호출부가 그리는 순서로 정한다.
 */
@Composable
internal fun ToppingArrangeCanvasDim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YGAtomicColors.Transparency.Black25),
    )
}
