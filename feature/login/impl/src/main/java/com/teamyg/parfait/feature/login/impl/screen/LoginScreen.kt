package com.teamyg.parfait.feature.login.impl.screen

import androidx.compose.foundation.layout.Arrangement
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
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.login.impl.component.AppleSignInButton
import com.teamyg.parfait.feature.login.impl.component.KakaoSignInButton
import com.teamyg.parfait.feature.login.impl.component.OnboardingPager
import com.teamyg.parfait.feature.login.impl.model.OnboardingPage
import com.teamyg.parfait.feature.login.impl.model.OnboardingPagesPreviewParameterProvider

@Composable
internal fun LoginScreen(
    pages: List<OnboardingPage>,
    onClickKakaoButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(all = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(45.dp))

        OnboardingPager(
            pages = pages,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        Spacer(modifier = Modifier.height(30.dp))

        KakaoSignInButton(
            onClick = onClickKakaoButton,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        AppleSignInButton(
            onClick = onClickKakaoButton,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@YGPreview
@Composable
private fun PreviewLoginScreen(
    @PreviewParameter(OnboardingPagesPreviewParameterProvider::class) pages: List<OnboardingPage>,
) = PreviewBox {
    LoginScreen(
        pages = pages,
        onClickKakaoButton = {},
        modifier = Modifier.fillMaxSize(),
    )
}
