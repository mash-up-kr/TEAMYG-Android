package com.teamyg.parfait.core.designsystem.component.ygloading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygdimoverlay.YGDimOverlay
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

const val YG_LOADING_OVERLAY_TEST_TAG = "yg_loading_overlay"

/**
 * 로띠가 Dim 위에 얹히므로 화면 테마와 무관하게 밝은 색 애셋을 쓴다.
 */
@Composable
fun YGLoadingOverlay(modifier: Modifier = Modifier) {
    YGDimOverlay(
        contentDescription = stringResource(R.string.yg_loading_overlay_description),
        modifier = modifier.testTag(YG_LOADING_OVERLAY_TEST_TAG),
    ) {
        YGLoadingLottie(art = YGLoadingArt.Light)
    }
}

@YGPreview
@Composable
private fun YGLoadingOverlayPreview() = PreviewBox {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "가려질 컨텐츠",
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray900,
        )
        YGLoadingOverlay()
    }
}
