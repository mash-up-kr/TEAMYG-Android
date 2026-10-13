package com.teamyg.parfait

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.analytics.ScreenViewTracker
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.analytics.AnalyticsEvent
import com.teamyg.parfait.core.analytics.AnalyticsEventType
import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.core.analytics.AnalyticsScreenType
import com.teamyg.parfait.deeplink.toAppLinkDeepLinkOrNull
import com.teamyg.parfait.domain.event.AppLinkDeepLinkEventBus
import com.teamyg.parfait.domain.event.PushDeepLinkEventBus
import com.teamyg.parfait.domain.event.SessionEventBus
import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink
import com.teamyg.parfait.domain.usecase.deeplink.ConsumeInstallReferrerDeepLinkUseCase
import com.teamyg.parfait.domain.usecase.session.HasActiveSessionUseCase
import com.teamyg.parfait.push.toPushDeepLinkOrNull
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

// API 26 에는 밝은 내비게이션 바 아이콘이 없어 대신 이 스크림이 깔린다(값은 androidx 기본값).
private val NavigationBarDarkScrim: Int = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var entryBuilders: Set<@JvmSuppressWildcards EntryProviderScope<NavKey>.(Navigator) -> Unit>

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var sessionEventBus: SessionEventBus

    @Inject
    lateinit var pushDeepLinkEventBus: PushDeepLinkEventBus

    @Inject
    lateinit var appLinkDeepLinkEventBus: AppLinkDeepLinkEventBus

    @Inject
    lateinit var consumeInstallReferrerDeepLink: ConsumeInstallReferrerDeepLinkUseCase

    @Inject
    lateinit var hasActiveSession: HasActiveSessionUseCase

    @Inject
    lateinit var screenViewTracker: ScreenViewTracker

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // savedInstanceState != null 은 구성 변경·프로세스 재생성이다 — 그때는 intent 가 이전
        // 인스턴스에서 이미 처리한 푸시 딥링크를 다시 들고 올 수 있어, 진짜 첫 실행일 때만 본다
        if (savedInstanceState == null) {
            consumePushDeepLink(intent)
            consumeAppLinkDeepLink(intent)
            // 콜드 스타트에서만 확인한다 — Install Referrer 는 "이번 설치"의 스토어 진입 정보라
            // onNewIntent(같은 프로세스가 살아있는 재진입)에서 다시 물어봐도 의미가 없다.
            lifecycleScope.launch {
                consumeInstallReferrerDeepLink()?.let(::publishAppLinkDeepLink)
            }
        }
        // light 는 바 배경이 밝다는 뜻이라 아이콘이 어두워진다. 다크모드를 따라가지 않는
        // 근거는 parfait/adr/0028-system-bar-light-fixed.md 에 있다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, NavigationBarDarkScrim),
        )
        setContent {
            YGCustomTheme {
                MainRoute(
                    navigator = navigator,
                    entryBuilders = entryBuilders,
                    sessionEventBus = sessionEventBus,
                    pushDeepLinkEventBus = pushDeepLinkEventBus,
                    appLinkDeepLinkEventBus = appLinkDeepLinkEventBus,
                    hasActiveSession = hasActiveSession,
                    screenViewTracker = screenViewTracker,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    // 앱이 이미 떠 있을 때(백그라운드 포함) 알림을 탭하면 새 인스턴스 대신 여기로 온다
    // (매니페스트의 launchMode="singleTop"). 콜드 스타트는 onCreate 의 intent 로 잡힌다.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumePushDeepLink(intent)
        consumeAppLinkDeepLink(intent)
    }

    // 소비한 뒤 반드시 비운다 — 안 비우면 화면 회전 없이도 재구성이 일어나는 경로(다크모드
    // 토글, 폰트·언어 설정 변경, 폴더블·멀티윈도우 리사이즈)에서 onCreate 가 같은 intent 를
    // 다시 받아 이미 처리한 딥링크를 또 post 하고, MainRoute 가 엉뚱하게 재이동한다.
    private fun consumePushDeepLink(intent: Intent) {
        val deepLink = intent.toPushDeepLinkOrNull() ?: return
        setIntent(Intent())
        deepLink.type?.let { type ->
            analyticsLogger.logEvent(
                AnalyticsEvent(
                    type = AnalyticsEventType.APP_PUSH_OPENED,
                    params = mapOf("type" to type.name),
                ),
            )
        }
        pushDeepLinkEventBus.post(deepLink)
    }

    /** [consumePushDeepLink] 와 같은 이유로 소비한 뒤 반드시 비운다. */
    private fun consumeAppLinkDeepLink(intent: Intent) {
        val deepLink = intent.toAppLinkDeepLinkOrNull() ?: return
        setIntent(Intent())
        publishAppLinkDeepLink(deepLink)
    }

    // 오픈 이벤트는 발행 지점에서 찍는다 — 로그인 대기를 지난 뒤에 찍으면 로그인 화면에서
    // 이탈한 유저는 링크로 열었는데도 집계에서 빠진다.
    private fun publishAppLinkDeepLink(deepLink: AppLinkDeepLink) {
        val destination = when (deepLink) {
            is AppLinkDeepLink.OpenGroupInvite -> AnalyticsScreenType.GROUP_INVITE_CODE
        }
        analyticsLogger.logEvent(
            AnalyticsEvent(
                type = AnalyticsEventType.APP_LINK_OPENED,
                params = mapOf("destination" to destination.screenId),
            ),
        )
        appLinkDeepLinkEventBus.post(deepLink)
    }
}
