package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * 세그멘테이션에 실패했을 때 보여주는 임시 화면.
 *
 * TODO: 디자인 확정 후 실제 에러 화면으로 교체 필요
 */
@Composable
internal fun SegmentationErrorScreen(
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(YGAtomicColors.Gray.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = "사진 편집에 실패했어요", // TODO: string resource 분리 및 디자인 확정 필요
            style = YGTheme.typography.body.b01SB,
            color = YGAtomicColors.Gray.Gray900,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "잠시 후 다시 시도해 주세요", // TODO: string resource 분리 및 디자인 확정 필요
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray600,
            textAlign = TextAlign.Center,
        )
        YGButton(
            text = "뒤로가기", // TODO: string resource 분리 및 디자인 확정 필요
            buttonType = YGButtonType.Medium.Primary,
            isEnabled = true,
            onClick = onClickBack,
        )
    }
}

@YGPreview
@Composable
private fun SegmentationErrorScreenPreview() = PreviewBox {
    SegmentationErrorScreen(
        onClickBack = {},
        modifier = Modifier.fillMaxSize(),
    )
}
