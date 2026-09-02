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
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.data.pushdeeplink.PushDeepLinkBus
import com.teamyg.parfait.domain.repository.session.SessionEventSource
import com.teamyg.parfait.pushdeeplink.toPushDeepLinkOrNull
import dagger.hilt.android.AndroidEntryPoint
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
    lateinit var sessionEventSource: SessionEventSource

    @Inject
    lateinit var pushDeepLinkBus: PushDeepLinkBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumePushDeepLink(intent)
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
                    sessionEventSource = sessionEventSource,
                    pushDeepLinkSource = pushDeepLinkBus,
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
    }

    private fun consumePushDeepLink(intent: Intent) {
        intent.toPushDeepLinkOrNull()?.let(pushDeepLinkBus::post)
    }
}
