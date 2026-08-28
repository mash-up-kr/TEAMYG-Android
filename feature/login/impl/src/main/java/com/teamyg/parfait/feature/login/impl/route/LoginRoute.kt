package com.teamyg.parfait.feature.login.impl.route

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
import com.teamyg.parfait.feature.intro.api.NavKeyTermAgree
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.feature.login.impl.R
import com.teamyg.parfait.feature.login.impl.model.OnboardingPage
import com.teamyg.parfait.feature.login.impl.screen.LoginScreen
import com.teamyg.parfait.feature.login.impl.util.KakaoLoginHelper
import com.teamyg.parfait.feature.login.impl.viewmodel.LoginError
import com.teamyg.parfait.feature.login.impl.viewmodel.LoginIntent
import com.teamyg.parfait.feature.login.impl.viewmodel.LoginSideEffect
import com.teamyg.parfait.feature.login.impl.viewmodel.LoginViewModel
import com.teamyg.parfait.feature.login.impl.viewmodel.toStringResource
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun LoginRoute(
    navigator: Navigator,
    kakaoLoginHelper: KakaoLoginHelper,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val activity = LocalActivity.current
    val toastPolicy = rememberYGToastPolicy()

    // 이펙트 수집은 컴포지션이 아니라 코루틴이라 그 안에서 `stringResource` 를 부를 수 없다.
    // `LocalContext.current.getString` 으로 우회하면 리소스 읽기가 컴포지션 밖으로 나가
    // 로케일·설정 변경 때 갱신되지 않는다(`LocalContextResourcesRead` 린트). 그래서 문구는
    // 여기서 미리 뽑아 두고 이펙트는 고르기만 한다
    val errorMessages = LoginError.entries.associateWith { it.toStringResource() }

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

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginSideEffect.NavigateToGroupList -> {
                    navigator.replaceAll(destination = NavKeyGroupList)
                }

                is LoginSideEffect.ShowError -> {
                    toastPolicy.showError(errorMessages.getValue(effect.error))
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

                    try {
                        when (
                            val result = kakaoLoginHelper.login(
                                activity = currentActivity,
                                forceAccountLogin = effect.forceAccountLogin,
                            )
                        ) {
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
                    } catch (e: CancellationException) {
                        // 설정 변경(회전 등)으로 컴포지션이 죽어 이 continuation 이 취소되면
                        // SDK 콜백이 돌아올 곳이 없다 — 로딩이 영구히 켜진 채 남는 것을 막는다
                        viewModel.processIntent(LoginIntent.LoginWithKakaoCancel)
                        throw e
                    }
                }
            }
        }
    }

    YGScaffoldV2(
        modifier = modifier,
        isLoading = state.isLoading,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
        // 제스처를 콘텐츠 **위에** 덮는 오버레이로 달면 카카오 버튼 탭까지 이 레이어가 먹는다.
        // 부모에 달면 자식이 먼저 히트 테스트를 받으므로, 소비자가 없는 빈 영역의 탭만 온다
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { viewModel.processIntent(LoginIntent.DebugDoubleTap) },
                        onLongPress = { viewModel.processIntent(LoginIntent.DebugLongPress) },
                    )
                },
        ) {
            LoginScreen(
                pages = tempPages,
                isLoading = state.isLoading,
                onClickKakaoButton = {
                    viewModel.processIntent(LoginIntent.LoginWithKakao)
                },
                // `fillMaxSize` 는 취향이 아니라 하중이다 — LoginScreen 의 Column 안에서
                // OnboardingPager 가 weight(1f) 을 쓰므로 높이가 안 잡히면 레이아웃이 접힌다
                modifier = Modifier.fillMaxSize(),
            )

            if (state.isDebugMode) {
                Text(
                    text = stringResource(R.string.login_debug_mode_badge),
                    style = YGTheme.typography.caption.c01R,
                    color = YGAtomicColors.Gray.Gray300,
                    // `clickable` 을 `padding` 앞에 둬야 여백까지 탭 영역이 된다. 12sp 글자에
                    // 사방 16dp 를 붙여야 최소 터치 타깃(48dp)에 닿는다 — 이 배지가 디버그
                    // 모드를 끄는 유일한 경로다
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable(onClickLabel = stringResource(R.string.login_debug_mode_disable_label)) {
                            viewModel.processIntent(LoginIntent.DisableDebugMode)
                        }.padding(YGTheme.layout.padding.padding6),
                )
            }
        }
    }
}
