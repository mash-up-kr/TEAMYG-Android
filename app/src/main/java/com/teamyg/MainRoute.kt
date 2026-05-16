package com.teamyg

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.teamyg.navigation.Navigator

@Composable
fun MainRoute(
    navigator: Navigator,
    entryBuilders: Set<EntryProviderScope<NavKey>.(Navigator) -> Unit>,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        entryDecorators = listOf(
            // Add the default decorators for managing scenes and saving state
            rememberSaveableStateHolderNavEntryDecorator(),
            // Then add the view model store decorator
            rememberViewModelStoreNavEntryDecorator(),
        ),
        backStack = navigator.backStack,
        onBack = navigator::onBack,
        entryProvider = entryProvider {
            entryBuilders.forEach { builder -> this.builder(navigator) }
        },
        modifier = modifier,
    )
}
