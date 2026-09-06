package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarClose
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.R

/**
 * 대상을 잘라내지 못했을 때의 화면(Figma `C-103-Error`).
 */
@Composable
internal fun SegmentationErrorScreen(
    onClickRetry: () -> Unit,
    onClickUseOriginal: () -> Unit,
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(YGAtomicColors.Gray.White)) {
        YGFloatingBarClose(
            onCloseClick = onClickClose,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // 아이콘·문구·버튼의 간격이 서로 달라 균일 배치를 쓰지 않는다
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(DesignSystemR.drawable.ic_warning_round),
                    contentDescription = null,
                    modifier = Modifier.size(SizeTokens.Size44.getDp()),
                    // drawable 자체는 검정이다 — 쓰는 쪽이 색을 정한다
                    colorFilter = ColorFilter.tint(YGAtomicColors.Cherry.Cherry600),
                )

                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap3))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
                ) {
                    Text(
                        text = stringResource(R.string.segmentation_error_title),
                        style = YGTheme.typography.title.t03SB,
                        color = YGAtomicColors.Gray.Gray900,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = stringResource(R.string.segmentation_error_description),
                        style = YGTheme.typography.body.b02R,
                        color = YGAtomicColors.Gray.Gray500,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap7))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
                    modifier = Modifier.width(BUTTON_WIDTH),
                ) {
                    YGButton(
                        text = stringResource(R.string.segmentation_error_retry),
                        buttonType = YGButtonType.Medium.Primary,
                        isEnabled = true,
                        onClick = onClickRetry,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    YGButton(
                        text = stringResource(R.string.segmentation_error_use_original),
                        buttonType = YGButtonType.Medium.Secondary,
                        isEnabled = true,
                        onClick = onClickUseOriginal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** 디자인 `C-103-Error` 실측값 */
private val BUTTON_WIDTH = 161.5.dp

@YGPreview
@Composable
private fun PreviewSegmentationErrorScreen() = PreviewBox {
    SegmentationErrorScreen(
        onClickRetry = {},
        onClickUseOriginal = {},
        onClickClose = {},
        modifier = Modifier.fillMaxSize(),
    )
}
