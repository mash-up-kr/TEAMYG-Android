package com.teamyg.parfait.feature.login.impl.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.ui.preview.PreviewBox
import com.teamyg.parfait.core.ui.preview.YGPreview
import com.teamyg.parfait.feature.login.impl.component.KakaoSignInButton

@Composable
internal fun LoginScreen(
    onClickKakaoButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(all = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // TODO Horizontal Pager

        Spacer(modifier = Modifier.weight(1f))

        KakaoSignInButton(
            onClick = onClickKakaoButton,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@YGPreview
@Composable
private fun PreviewLoginScreen() = PreviewBox {
    LoginScreen(
        onClickKakaoButton = {},
        modifier = Modifier.fillMaxSize(),
    )
}
