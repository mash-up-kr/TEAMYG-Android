package com.teamyg.parfait.feature.login.impl.route

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.feature.groups.home.api.NavKeyGroupHome
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.feature.login.impl.model.OnboardingPage
import com.teamyg.parfait.feature.login.impl.screen.LoginScreen
import com.teamyg.parfait.feature.login.impl.util.KakaoLoginHelper
import com.teamyg.parfait.feature.login.impl.viewmodel.LoginIntent
import com.teamyg.parfait.feature.login.impl.viewmodel.LoginSideEffect
import com.teamyg.parfait.feature.login.impl.viewmodel.LoginViewModel

@Composable
fun LoginRoute(
    navigator: Navigator,
    kakaoLoginHelper: KakaoLoginHelper,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val activity = LocalActivity.current

    val tempPages: List<OnboardingPage> = remember {
        listOf(
            OnboardingPage(
                description = "매일 새로운 캔버스에 하루하루 다르게 기록해요",
                painterResourceId = DesignSystemR.drawable.image_onboarding_1,
            ),
            OnboardingPage(
                description = "오늘 찍은 사진을 친구들과 함께 캔버스에 붙여요",
                painterResourceId = DesignSystemR.drawable.image_onboarding_2,
            ),
            OnboardingPage(
                description = "서로의 하루가 겹겹이 쌓여,\n하나의 캔버스로 완성돼요",
                painterResourceId = DesignSystemR.drawable.image_onboarding_1,
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

                is LoginSideEffect.RequestLoginWithKakao -> {
                    activity?.let {
                        when (val result = kakaoLoginHelper.login(activity)) {
                            is KakaoLoginResult.Success ->
                                viewModel.processIntent(LoginIntent.LoginWithKakaoSuccess(result.token))

                            is KakaoLoginResult.Failure ->
                                viewModel.processIntent(LoginIntent.LoginWithKakaoFailure(result.throwable))

                            is KakaoLoginResult.Cancel ->
                                viewModel.processIntent(LoginIntent.LoginWithKakaoCancel)
                        }
                    }
                }
            }
        }
    }

    ResultEffect<String> { returnText ->
        Toast.makeText(activity, returnText, Toast.LENGTH_LONG).show()
    }

    LoginScreen(
        pages = tempPages,
        onClickKakaoButton = {
            viewModel.processIntent(LoginIntent.LoginWithKakao)
        },
        modifier = modifier,
    )
}
