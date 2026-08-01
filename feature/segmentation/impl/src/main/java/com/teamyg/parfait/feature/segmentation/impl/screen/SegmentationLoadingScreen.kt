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
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

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
            .padding(
                start = YGTheme.layout.padding.padding7,
                end = YGTheme.layout.padding.padding7,
                top = YGTheme.layout.padding.padding6,
                bottom = YGTheme.layout.padding.padding6,
            ),
    ) {
        YGCircleButton(
            iconResource = DesignSystemR.drawable.ic_close,
            type = YGCircleButtonType.Default,
            contentDescription = null,
            onClick = onClickClose,
            modifier = Modifier.align(Alignment.End),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            // TODO: 로띠 넣을 예정
            CircularProgressIndicator(color = YGAtomicColors.Cherry.Cherry100)

            Spacer(modifier = Modifier.height(11.dp))

            Text(
                text = "사진을 편집하고 있어요", // TODO: string resource 분리 필요
                style = YGTheme.typography.title.t03SB,
                color = YGAtomicColors.Gray.Gray900,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "잠시만 기다려주세요...", // TODO: string resource 분리 필요
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
