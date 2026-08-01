package com.teamyg.parfait.feature.segmentation.impl.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
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
            .background(YGAtomicColors.Gray.Gray850)
            .padding(
                horizontal = YGTheme.layout.padding.padding5,
                vertical = YGTheme.layout.padding.padding3,
            ),
    ) {
        Image(
            painter = painterResource(DesignSystemR.drawable.ic_info_round),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = YGAtomicColors.Soda.Soda500),
            modifier = Modifier.size(GuideBannerDefaults.IconSize),
        )

        Spacer(modifier = Modifier.width(YGTheme.layout.gap.gap2))

        Text(
            text = "토핑으로 사용할 대상을 하나 선택해 주세요", // TODO: string resource 분리 필요
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.White,
        )
    }
}

internal object GuideBannerDefaults {
    val IconSize: Dp = SizeTokens.Size28.getDp()
}

@YGPreview
@Composable
private fun GuideBannerPreview() = PreviewBox {
    GuideBanner(modifier = Modifier.fillMaxWidth())
}
