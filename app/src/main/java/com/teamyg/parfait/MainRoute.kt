package com.teamyg.parfait

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.ui.LocalSharedTransitionScope
import com.teamyg.parfait.host.GeneralLoadingHost
import com.teamyg.parfait.viewmodel.MainViewModel

@Composable
fun MainRoute(
    navigator: Navigator,
    entryBuilders: Set<EntryProviderScope<NavKey>.(Navigator) -> Unit>,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                entryProvider = entryProvider {
                    entryBuilders.forEach { builder -> this.builder(navigator) }
                },
            )
        }

        GeneralLoadingHost(state = state.loadingState)
    }
}
