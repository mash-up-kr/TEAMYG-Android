package com.teamyg.parfait.feature.groups.canvas.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasBGEdit
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasEdit
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageAdd
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSelect
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasBGEditRoute
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasEditRoute
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasImageAddRoute
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasImageSelectRoute
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.canvas.api.NavKeyCanvasMove
import com.teamyg.parfait.feature.canvas.impl.route.CanvasMoveRoute

fun EntryProviderScope<NavKey>.featureCanvasEntryBuilder(navigator: Navigator) {
    entry<NavKeyCanvasImageAdd> {
        YGScaffold { innerPadding ->
            CanvasImageAddRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    entry<NavKeyCanvasBGEdit> {
        YGScaffold { innerPadding ->
            CanvasBGEditRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
    entry<NavKeyCanvasEdit> { navKey ->
        YGScaffold { innerPadding ->
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
        YGScaffold { innerPadding ->
            CanvasImageSelectRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    entry<NavKeyCanvasMove> {
        YGScaffold { innerPadding ->
            CanvasMoveRoute(
                image = it.imageUri,
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
