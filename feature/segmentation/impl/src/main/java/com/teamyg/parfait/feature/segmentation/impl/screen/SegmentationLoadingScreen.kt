package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingArt
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingLottie
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/** 디자인 `C-101-Loading`. 대상을 인식하는 동안 화면 전체를 덮는다 */
@Composable
internal fun SegmentationLoadingScreen(
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YGAtomicColors.Gray.White),
    ) {
        YGCircleButton(
            iconResource = DesignSystemR.drawable.ic_close,
            type = YGCircleButtonType.Default,
            contentDescription = null,
            onClick = onClickClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = YGTheme.layout.padding.padding6,
                    end = YGTheme.layout.padding.padding7,
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap5),
            modifier = Modifier.align(Alignment.Center),
        ) {
            YGLoadingLottie(art = YGLoadingArt.Dark)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
            ) {
                Text(
                    text = stringResource(R.string.segmentation_loading_title),
                    style = YGTheme.typography.title.t03SB,
                    color = YGAtomicColors.Gray.Gray900,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = stringResource(R.string.segmentation_loading_description),
                    style = YGTheme.typography.body.b02R,
                    color = YGAtomicColors.Gray.Gray500,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewSegmentationLoadingScreen() = PreviewBox {
    SegmentationLoadingScreen(onClickClose = {})
}
