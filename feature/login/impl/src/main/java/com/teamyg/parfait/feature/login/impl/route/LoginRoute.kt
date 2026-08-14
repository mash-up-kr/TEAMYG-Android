package com.teamyg.parfait.feature.login.impl.route

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.ui.CollectAppError
import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
import com.teamyg.parfait.feature.intro.api.NavKeyTermAgree
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
                painterResourceId = DesignSystemR.drawable.image_onboarding_3,
            ),
        )
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectAppError(viewModel)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginSideEffect.NavigateToGroupList -> {
                    navigator.clearBackStack()
                    navigator.goTo(destination = NavKeyGroupList)
                }

                is LoginSideEffect.NavigateToTermAgree -> {
                    navigator.goTo(
                        destination = NavKeyTermAgree(registrationToken = effect.registrationToken),
                    )
                }

                is LoginSideEffect.RequestLoginWithKakao -> {
                    // activity 가 null 이면 로딩이 켜진 채 영영 남는다 — 실패로 닫는다
                    val currentActivity = activity
                    if (currentActivity == null) {
                        viewModel.processIntent(
                            LoginIntent.LoginWithKakaoFailure(IllegalStateException("Activity 가 없다")),
                        )
                        return@collect
                    }

                    when (val result = kakaoLoginHelper.login(currentActivity)) {
                        is KakaoLoginResult.Success ->
                            viewModel.processIntent(
                                LoginIntent.LoginWithKakaoSuccess(
                                    idToken = result.idToken,
                                    nonce = result.nonce,
                                ),
                            )

                        is KakaoLoginResult.Failure ->
                            viewModel.processIntent(LoginIntent.LoginWithKakaoFailure(result.throwable))

                        is KakaoLoginResult.Cancel ->
                            viewModel.processIntent(LoginIntent.LoginWithKakaoCancel)
                    }
                }
            }
        }
    }

    LoginScreen(
        pages = tempPages,
        isLoading = state.isLoading,
        onClickKakaoButton = {
            viewModel.processIntent(LoginIntent.LoginWithKakao)
        },
        modifier = modifier,
    )
}
