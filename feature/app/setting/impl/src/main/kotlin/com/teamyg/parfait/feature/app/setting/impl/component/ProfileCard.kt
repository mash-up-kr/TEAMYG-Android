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

/**
 * @param nickname `null` 은 SSoT 가 아직 값을 방출하지 않은 로딩 상태다(빈 문자열이 아니다).
 * @param loginProviderText 이미 화면에 보일 문구로 매핑된 값. `LoginProvider` 도메인 타입에서
 *   `core:ui`의 `toStringResource()`(ADR-0016)로 변환하는 것은 호출부(Screen)의 책임이다.
 *   `null` 은 [nickname] 과 같은 이유로 로딩이다.
 */
@Composable
internal fun ProfileCard(
    nickname: String?,
    loginProviderText: String?,
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
                text = nickname ?: stringResource(R.string.setting_profile_loading),
                style = YGTheme.typography.title.t03SB,
                color = YGAtomicColors.Gray.Gray900,
            )
            Text(
                text = loginProviderText ?: stringResource(R.string.setting_profile_loading),
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
        loginProviderText = "카카오",
        modifier = Modifier.padding(20.dp),
    )
}
