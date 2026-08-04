package com.teamyg.parfait.feature.app.setting.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.app.setting.impl.R

@Composable
internal fun ProfileCard(
    nickname: String,
    loginProvider: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = YGAtomicColors.Transparency.White75,
                shape = RectangleShape,
            ).border(
                width = 1.dp,
                color = YGAtomicColors.Cherry.Cherry100,
                shape = RectangleShape,
            ).padding(YGTheme.layout.padding.padding6),
    ) {
        YGLabel(text = stringResource(R.string.setting_profile_label))
        Column(
            verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
        ) {
            Text(
                text = nickname,
                style = YGTheme.typography.title.t03SB,
                color = YGAtomicColors.Gray.Gray900,
            )
            Text(
                text = loginProvider,
                style = YGTheme.typography.caption.c01R,
                color = YGAtomicColors.Gray.Gray500,
            )
        }
    }
}

@YGPreview
@Composable
private fun ProfileCardPreview() = PreviewBox {
    ProfileCard(
        nickname = "아니야나그런데기니야",
        loginProvider = "Kakao",
        modifier = Modifier.padding(20.dp),
    )
}
