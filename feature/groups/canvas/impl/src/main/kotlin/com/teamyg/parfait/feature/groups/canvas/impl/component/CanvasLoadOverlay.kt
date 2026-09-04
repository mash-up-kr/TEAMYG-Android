package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygdimoverlay.YGDimOverlay
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingArt
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingLottie
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun CanvasLoadingOverlay(modifier: Modifier = Modifier) {
    // 한 덩어리로 읽히므로 안내 문구까지 합친다 — 빼면 그 말이 가장 필요한 사람에게 안 닿는다
    val description = stringResource(R.string.canvas_main_loading_title) + " " +
        stringResource(R.string.canvas_main_loading_description)

    YGDimOverlay(
        contentDescription = description,
        modifier = modifier,
    ) {
        YGLoadingLottie(art = YGLoadingArt.Topping)

        OverlayMessage(
            title = stringResource(R.string.canvas_main_loading_title),
            description = stringResource(R.string.canvas_main_loading_description),
        )
    }
}

/** 버튼이 눌려야 하므로 판 전체를 한 덩어리로 읽히지 않는다 */
@Composable
internal fun CanvasLoadErrorOverlay(
    onClickRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGDimOverlay(modifier = modifier) {
        Image(
            painter = painterResource(DesignSystemR.drawable.ic_warning_round),
            contentDescription = null,
            // drawable 자체는 검정이다 — 쓰는 쪽이 색을 정한다
            colorFilter = ColorFilter.tint(YGAtomicColors.Cherry.Cherry600),
            modifier = Modifier.size(SizeTokens.Size44.getDp()),
        )

        OverlayMessage(
            title = stringResource(R.string.canvas_main_load_error_title),
            description = stringResource(R.string.canvas_main_load_error_description),
        )

        YGButton(
            text = stringResource(R.string.canvas_main_load_error_retry),
            buttonType = YGButtonType.Medium.Secondary,
            isEnabled = true,
            onClick = onClickRetry,
        )
    }
}

@Composable
private fun OverlayMessage(
    title: String,
    description: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
        modifier = Modifier.padding(vertical = YGTheme.layout.gap.gap3),
    ) {
        Text(
            text = title,
            style = YGTheme.typography.title.t03SB,
            color = YGAtomicColors.Gray.White,
            textAlign = TextAlign.Center,
        )

        Text(
            text = description,
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray300,
            textAlign = TextAlign.Center,
        )
    }
}

@YGPreview
@Composable
private fun CanvasLoadingOverlayPreview() = PreviewBox {
    CanvasLoadingOverlay()
}

@YGPreview
@Composable
private fun CanvasLoadErrorOverlayPreview() = PreviewBox {
    CanvasLoadErrorOverlay(onClickRetry = {})
}
