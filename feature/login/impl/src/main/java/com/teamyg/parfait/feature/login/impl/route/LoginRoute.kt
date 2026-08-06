package com.teamyg.parfait.feature.login.impl.route

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.feature.groups.home.api.NavKeyGroupHome
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.feature.login.impl.R
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

    val onboardingDescription1 = stringResource(R.string.login_onboarding_description_1)
    val onboardingDescription2 = stringResource(R.string.login_onboarding_description_2)
    val onboardingDescription3 = stringResource(R.string.login_onboarding_description_3)

    val tempPages: List<OnboardingPage> = remember(
        onboardingDescription1,
        onboardingDescription2,
        onboardingDescription3,
    ) {
        listOf(
            OnboardingPage(
                description = onboardingDescription1,
                painterResourceId = DesignSystemR.drawable.image_onboarding_1,
            ),
            OnboardingPage(
                description = onboardingDescription2,
                painterResourceId = DesignSystemR.drawable.image_onboarding_2,
            ),
            OnboardingPage(
                description = onboardingDescription3,
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
