package com.teamyg.canvas.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.canvas.api.NavKeyCanvasEdit
import com.teamyg.canvas.api.NavKeyCanvasImageAdd
import com.teamyg.canvas.api.NavKeyCanvasImageSelect
import com.teamyg.canvas.impl.route.CanvasEditRoute
import com.teamyg.canvas.impl.route.CanvasImageAddRoute
import com.teamyg.canvas.impl.route.CanvasImageSelectRoute
import com.teamyg.navigation.Navigator

fun EntryProviderScope<NavKey>.featureCanvasEntryBuilder(navigator: Navigator) {
    entry<NavKeyCanvasImageAdd> {
        Scaffold { innerPadding ->
            CanvasImageAddRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
    entry<NavKeyCanvasEdit> { navKey ->
        Scaffold { innerPadding ->
            CanvasEditRoute(
                imageUri = navKey.imageUri,
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    entry<NavKeyCanvasImageSelect> {
        Scaffold { innerPadding ->
            CanvasImageSelectRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
