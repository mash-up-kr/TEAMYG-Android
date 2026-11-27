package com.teamyg.parfait.feature.app.setting.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.gesture.HiddenTapCounter
import com.teamyg.parfait.feature.app.setting.impl.R

/**
 * @param nickname `null` 은 SSoT 가 아직 값을 방출하지 않은 로딩 상태다(빈 문자열이 아니다).
 * @param loginProviderText 이미 화면에 보일 문구로 매핑된 값. `LoginProvider` 도메인 타입에서
 *   `core:ui`의 `toStringResource()`(ADR-0016)로 변환하는 것은 호출부(Screen)의 책임이다.
 *   `null` 은 [nickname] 과 같은 이유로 로딩이다.
 * @param onFiveTaps 카드를 5번 탭하면 불린다(혼자 공간 숨은 진입점).
 */
@Composable
internal fun ProfileCard(
    nickname: String?,
    loginProviderText: String?,
    onFiveTaps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val counter = remember { HiddenTapCounter() }
    val currentOnFiveTaps by rememberUpdatedState(onFiveTaps)

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
            ).pointerInput(Unit) {
                // 피드백 없이 탭만 센다 — 숨은 진입점이라 ripple 도 두지 않는다
                detectTapGestures { if (counter.tap()) currentOnFiveTaps() }
            }.padding(YGTheme.layout.padding.padding6),
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
        loginProviderText = "Kakao",
        onFiveTaps = {},
        modifier = Modifier.padding(20.dp),
    )
}
