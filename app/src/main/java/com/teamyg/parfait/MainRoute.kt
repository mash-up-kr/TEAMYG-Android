package com.teamyg.parfait

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.teamyg.parfait.core.navigation.NavTransition
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.ui.LocalSharedTransitionScope
import com.teamyg.parfait.domain.model.session.SessionEvent
import com.teamyg.parfait.domain.repository.session.SessionEventSource
import com.teamyg.parfait.feature.login.api.NavKeyLogin

@Composable
fun MainRoute(
    navigator: Navigator,
    entryBuilders: Set<EntryProviderScope<NavKey>.(Navigator) -> Unit>,
    sessionEventSource: SessionEventSource,
    modifier: Modifier = Modifier,
) {
    // 세션 사건은 화면 하나가 결정할 수 없다. 여기 한 곳에서만 수집한다 —
    // 화면마다 구독하면 한 이벤트로 이동이 여러 번 일어난다.
    LaunchedEffect(Unit) {
        sessionEventSource.events.collect { event ->
            when (event) {
                SessionEvent.ForcedLogout -> {
                    navigator.replaceAll(NavKeyLogin)
                }
            }
        }
    }

    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
                entryDecorators = listOf(
                    // NavEntry Lifecycle 동안 유효한 SaveableState 를 만드는 Decorator
                    rememberSaveableStateHolderNavEntryDecorator(),
                    // NavEntry Lifecycle 동안 유효한 ViewModel 를 만드는 Decorator
                    rememberViewModelStoreNavEntryDecorator(),
                    // NavEntry 범위마다 공통 ReturnEventBus 객체를 CompositionLocal 로 제공하는 Decorator
                    // Returning Result 를 위해 사용하는 EventBus 를 feature impl 모듈들의 Composable 에서
                    // LocalResultEventBus.current 로 가져올 수 있게 됨
                    rememberResultEventBusNavEntryDecorator(),
                ),
                backStack = navigator.backStack,
                onBack = navigator::onBack,
                // 다르게 움직여야 하는 화면은 그 화면의 entry 에 NavTransition metadata 를 붙인다
                transitionSpec = { NavTransition.Default.push(this) },
                popTransitionSpec = { NavTransition.Default.pop(this) },
                predictivePopTransitionSpec = { swipeEdge ->
                    NavTransition.Default.predictivePop(this, swipeEdge)
                },
                entryProvider = entryProvider {
                    entryBuilders.forEach { builder -> this.builder(navigator) }
                },
            )
        }
    }
}
