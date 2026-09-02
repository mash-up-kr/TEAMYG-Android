package com.teamyg.parfait.feature.login.impl.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.login.impl.component.KakaoSignInButton
import com.teamyg.parfait.feature.login.impl.component.OnboardingPager
import com.teamyg.parfait.feature.login.impl.model.OnboardingPage
import com.teamyg.parfait.feature.login.impl.model.OnboardingPagesPreviewParameterProvider

@Composable
internal fun LoginScreen(
    pages: List<OnboardingPage>,
    isLoading: Boolean,
    onClickKakaoButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(top = 45.dp, bottom = YGTheme.layout.padding.padding1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingPager(
            pages = pages,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 35.dp, end = 34.dp),
        )

        Spacer(modifier = Modifier.height(30.dp)) // 30.dp가 gap 없음

        KakaoSignInButton(
            onClick = onClickKakaoButton,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = YGTheme.layout.padding.padding7),
        )

        Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))
    }
}

@YGPreview
@Composable
private fun PreviewLoginScreen(
    @PreviewParameter(OnboardingPagesPreviewParameterProvider::class) pages: List<OnboardingPage>,
) = PreviewBox {
    LoginScreen(
        pages = pages,
        isLoading = false,
        onClickKakaoButton = {},
        modifier = Modifier.fillMaxSize(),
    )
}
