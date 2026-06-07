package com.teamyg.login.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LoginScreen(
    onClickLoginButton: () -> Unit,
    onClickKakaoButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "login",
            modifier = Modifier.clickable { onClickLoginButton() },
        )
        Text(
            text = "kakao login",
            modifier = Modifier.clickable { onClickKakaoButton() },
        )
    }
}
