package com.teamyg.parfait.feature.groups.canvas.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasBGEdit
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasEdit
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSelect
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasBGEditRoute
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasEditRoute
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasMainRoute
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasImageSelectRoute
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMove
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasToppingPlace
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasMoveRoute
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasToppingPlaceRoute

fun EntryProviderScope<NavKey>.featureCanvasEntryBuilder(navigator: Navigator) {
    entry<NavKeyCanvasMain> { navKey ->
        YGScaffold { innerPadding ->
            CanvasMainRoute(
                groupId = navKey.groupId,
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    entry<NavKeyCanvasBGEdit> { navKey ->
        YGScaffold { innerPadding ->
            CanvasBGEditRoute(
                groupId = navKey.groupId,
                parfaitId = navKey.parfaitId,
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

    entry<NavKeyCanvasToppingPlace> { navKey ->
        CanvasToppingPlaceRoute(
            key = navKey,
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
