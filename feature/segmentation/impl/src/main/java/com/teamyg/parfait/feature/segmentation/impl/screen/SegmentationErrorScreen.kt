package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarClose
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.R

/**
 * 대상을 잘라내지 못했을 때의 화면(Figma `C-103-Error`).
 *
 * 재시도·원본 사용 버튼이 없는 것은 정책과 갈리는 자리다
 * (`specs/2026-08-23-c103-multi-subject-selection.md` 「실패 화면」 절).
 */
@Composable
internal fun SegmentationErrorScreen(
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
            ) {
                Image(
                    painter = painterResource(DesignSystemR.drawable.ic_warning_round),
                    contentDescription = null,
                    modifier = Modifier.size(SizeTokens.Size44.getDp()),
                    // drawable 자체는 검정이다 — 쓰는 쪽이 색을 정한다
                    colorFilter = ColorFilter.tint(YGAtomicColors.Cherry.Cherry600),
                )

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
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewSegmentationErrorScreen() = PreviewBox {
    SegmentationErrorScreen(
        onClickClose = {},
        modifier = Modifier.fillMaxSize(),
    )
}
