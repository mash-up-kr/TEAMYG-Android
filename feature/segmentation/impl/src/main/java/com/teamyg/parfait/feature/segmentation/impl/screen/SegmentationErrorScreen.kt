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
 * 상단 바에 닫기만 두는 것은 디자인이 정한 것이다. 재시도·원본 사용 버튼은 없고 문구로만 안내한다 —
 * 위키 [[누끼-따기]] 가 말하는 "재시도 또는 원본 사용 옵션"과 갈리는 자리이며 그 판단은
 * `synthesis/open-questions.md` OQ-P-003 ① 에 남아 있다.
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
