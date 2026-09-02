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
 *
 * [title]·[description]은 실패 원인에 따라 호출부(SegmentationRoute)가 고른다.
 *
 * ⚠️ 재시도 버튼은 디자인 검토를 받으려고 먼저 놓은 시안이다
 * (`specs/2026-09-02-segmentation-module-install.md` 「재시도」 절). 검토 결과에 따라 사라지거나
 * 모양이 바뀔 수 있어 새 컴포넌트를 만들지 않았다.
 */
@Composable
internal fun SegmentationErrorScreen(
    title: String,
    description: String,
    onClickRetry: () -> Unit,
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
                        text = title,
                        style = YGTheme.typography.title.t03SB,
                        color = YGAtomicColors.Gray.Gray900,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = description,
                        style = YGTheme.typography.body.b02R,
                        color = YGAtomicColors.Gray.Gray500,
                        textAlign = TextAlign.Center,
                    )
                }

                YGButton(
                    text = stringResource(R.string.segmentation_error_retry),
                    buttonType = YGButtonType.Medium.Primary,
                    isEnabled = true,
                    onClick = onClickRetry,
                )
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewSegmentationErrorScreen() = PreviewBox {
    SegmentationErrorScreen(
        title = stringResource(R.string.segmentation_error_title),
        description = stringResource(R.string.segmentation_error_description),
        onClickRetry = {},
        onClickClose = {},
        modifier = Modifier.fillMaxSize(),
    )
}
