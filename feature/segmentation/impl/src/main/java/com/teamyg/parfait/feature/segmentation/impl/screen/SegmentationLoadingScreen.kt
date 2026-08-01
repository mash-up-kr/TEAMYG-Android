package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * 세그멘테이션이 끝날 때까지 보여주는 임시 로딩 화면.
 *
 * TODO: 디자인 확정 후 실제 로딩 화면으로 교체 필요
 */
@Composable
internal fun SegmentationLoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(YGAtomicColors.Gray.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator(color = YGAtomicColors.Cherry.Cherry100)
        Text(
            text = "사진을 다듬는 중이에요", // TODO: string resource 분리 및 디자인 확정 필요
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray700,
        )
    }
}

@YGPreview
@Composable
private fun SegmentationLoadingScreenPreview() = PreviewBox {
    SegmentationLoadingScreen(modifier = Modifier.fillMaxSize())
}
