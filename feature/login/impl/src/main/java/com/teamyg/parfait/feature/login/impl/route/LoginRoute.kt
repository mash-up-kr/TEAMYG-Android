package com.teamyg.parfait.feature.login.impl.route

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.home.api.NavKeyGroupHome
import com.teamyg.parfait.feature.login.impl.model.OnboardingPage
import com.teamyg.parfait.feature.login.impl.viewmodel.LoginIntent
import com.teamyg.parfait.feature.login.impl.screen.LoginScreen
import com.teamyg.parfait.feature.login.impl.viewmodel.LoginSideEffect
import com.teamyg.parfait.feature.login.impl.viewmodel.LoginViewModel

@Composable
fun LoginRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val tempPages: List<OnboardingPage> = remember {
        listOf(
            OnboardingPage(
                title = "매일 다르게 채우는 하루",
                description = "매일 새로운 캔버스가 생성돼요\n하루하루 다르게 기록해요",
                painterResourceId = null,
            ),
            OnboardingPage(
                title = "평범한 일상이 토핑으로",
                description = "오늘 찍은 사진을 누끼 스티커로 만들고,\n친구들과 함께 캔버스에 붙여요",
                painterResourceId = null,
            ),
            OnboardingPage(
                title = "완성된 하나의 파르페",
                description = "서로의 하루가 겹겹이 쌓여,\n하나의 캔버스로 완성돼요",
                painterResourceId = null,
            ),
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginSideEffect.NavigateToNext -> {
                    navigator.goTo(
                        destination = NavKeyGroupHome(
                            groupId = 1231,
                        ),
                    )
                }
            }
        }
    }

    ResultEffect<String> { returnText ->
        Toast.makeText(context, returnText, Toast.LENGTH_LONG).show()
    }

    LoginScreen(
        pages = tempPages,
        onClickKakaoButton = {
            viewModel.processIntent(LoginIntent.LoginWithKakao)
        },
        modifier = modifier,
    )
}
