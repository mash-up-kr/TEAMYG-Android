package com.teamyg.parfait.feature.intro.impl.splash

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.ui.component.modal.YGModalForceUpdate
import com.teamyg.parfait.core.ui.component.modal.YGModalOptionalUpdate
import com.teamyg.parfait.core.util.android.extension.tryStartViewIntent
import com.teamyg.parfait.feature.intro.impl.R
import com.teamyg.parfait.feature.intro.impl.model.PLAY_STORE_PACKAGE
import com.teamyg.parfait.feature.intro.impl.model.UpdateLink
import com.teamyg.parfait.feature.intro.impl.model.storeLinkCandidates
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
import com.teamyg.parfait.feature.login.api.NavKeyLogin

@Composable
fun SplashRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SplashSideEffect.NavigateToLogin -> navigator.replaceAll(destination = NavKeyLogin)

                SplashSideEffect.NavigateToGroupList -> navigator.replaceAll(destination = NavKeyGroupList)

                is SplashSideEffect.OpenStore -> {
                    val opened = context.openUpdateLink(storeLinkCandidates(effect.storeUrl, context.packageName))
                    // YGToastHost는 모달 Dialog 창 아래에 그려져 딤에 가려지므로 시스템 Toast를 쓴다
                    if (!opened) {
                        Toast.makeText(context, R.string.splash_store_open_failed, Toast.LENGTH_LONG).show()
                    }
                }

                SplashSideEffect.QuitApp -> activity?.finishAndRemoveTask()
            }
        }
    }

    YGScaffoldV2(
        modifier = modifier,
        // 로띠는 시스템바를 비켜난 영역이 아니라 화면의 정중앙에 있어야 한다
        contentWindowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0),
    ) { innerPadding ->
        SplashScreen(
            onAnimationFinished = { viewModel.processIntent(SplashIntent.AnimationFinished) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }

    if (state.isForceUpdateVisible) {
        YGModalForceUpdate(
            onQuit = { viewModel.processIntent(SplashIntent.QuitClick) },
            onUpdate = { viewModel.processIntent(SplashIntent.UpdateClick) },
        )
    }

    if (state.isOptionalUpdateVisible) {
        YGModalOptionalUpdate(
            onLater = { viewModel.processIntent(SplashIntent.LaterClick) },
            onUpdate = { viewModel.processIntent(SplashIntent.UpdateClick) },
        )
    }
}

// 어떤 링크를 어떤 순서로, 브라우저로 다시 열어도 되는지는 storeLinkCandidates 가 정한다
private fun Context.openUpdateLink(links: List<UpdateLink>): Boolean = links.any { link ->
    tryStartViewIntent(link.uri, packageName = PLAY_STORE_PACKAGE) ||
        (link.opensInBrowser && tryStartViewIntent(link.uri))
}
