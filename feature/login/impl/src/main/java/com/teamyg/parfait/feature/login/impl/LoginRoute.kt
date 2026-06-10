package com.teamyg.parfait.feature.login.impl

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.feature.grouphome.api.NavigationKey
import com.teamyg.parfait.core.navigation.Navigator

@Composable
fun LoginRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    ResultEffect<String> { returnText ->
        Toast.makeText(context, returnText, Toast.LENGTH_LONG).show()
    }

    LoginScreen(
        onClickLoginButton = {
            navigator.goTo(NavigationKey(groupId = 1231))
        },
        onClickKakaoButton = {
            viewModel.processIntent(LoginIntent.LoginWithKakao)
        },
        modifier = modifier,
    )
}
