package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarClose
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.R

/**
 * 세그멘테이션이 끝날 때까지 보여주는 로딩 화면.
 */
@Composable
internal fun SegmentationLoadingScreen(
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
            // TODO: 로띠 넣을 예정
            CircularProgressIndicator(color = YGAtomicColors.Cherry.Cherry100)

            Spacer(modifier = Modifier.height(11.dp))

            Text(
                text = stringResource(R.string.segmentation_loading_title),
                style = YGTheme.typography.title.t03SB,
                color = YGAtomicColors.Gray.Gray900,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(R.string.segmentation_loading_description),
                style = YGTheme.typography.body.b02R,
                color = YGAtomicColors.Gray.Gray500,
            )
        }
    }
}

@YGPreview
@Composable
private fun SegmentationLoadingScreenPreview() = PreviewBox {
    SegmentationLoadingScreen(
        onClickClose = {},
        modifier = Modifier.fillMaxSize(),
    )
}
