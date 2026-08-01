package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarClose
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/**
 * 세그멘테이션에 실패했을 때 보여주는 화면. [SegmentationLoadingScreen] 과 같은 구조로,
 * 로딩 인디케이터 자리에 경고 아이콘이 들어간다.
 */
@Composable
internal fun SegmentationErrorScreen(
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(YGAtomicColors.Gray.White)
            .padding(bottom = YGTheme.layout.padding.padding6),
    ) {
        YGFloatingBarClose(
            onCloseClick = onClickClose,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = YGTheme.layout.padding.padding7),
        ) {
            Image(
                painter = painterResource(DesignSystemR.drawable.ic_warning_round),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = YGAtomicColors.Cherry.Cherry600),
            )

            Spacer(modifier = Modifier.height(11.dp))

            Text(
                text = stringResource(R.string.segmentation_error_title),
                style = YGTheme.typography.title.t03SB,
                color = YGAtomicColors.Gray.Gray900,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(R.string.segmentation_error_description),
                style = YGTheme.typography.body.b02R,
                color = YGAtomicColors.Gray.Gray500,
            )
        }
    }
}

@YGPreview
@Composable
private fun SegmentationErrorScreenPreview() = PreviewBox {
    SegmentationErrorScreen(
        onClickClose = {},
        modifier = Modifier.fillMaxSize(),
    )
}
