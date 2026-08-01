package com.teamyg.parfait.feature.segmentation.impl.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/**
 * 화면 상단에서 사용법을 안내하는 배너.
 */
@Composable
internal fun GuideBanner(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(GuideBannerDefaults.Height)
            .background(YGAtomicColors.Gray.Gray850)
            .padding(horizontal = GuideBannerDefaults.HorizontalPadding),
    ) {
        Image(
            painter = painterResource(DesignSystemR.drawable.ic_info_round),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = YGAtomicColors.Soda.Soda500),
            modifier = Modifier.size(GuideBannerDefaults.IconSize),
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "토핑으로 사용할 대상을 하나 선택해 주세요", // TODO: string resource 분리 필요
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.White,
        )
    }
}

internal object GuideBannerDefaults {
    val Height: Dp = 44.dp
    val IconSize: Dp = 28.dp
    val HorizontalPadding: Dp = 12.dp
}

@YGPreview
@Composable
private fun GuideBannerPreview() = PreviewBox {
    GuideBanner()
}
