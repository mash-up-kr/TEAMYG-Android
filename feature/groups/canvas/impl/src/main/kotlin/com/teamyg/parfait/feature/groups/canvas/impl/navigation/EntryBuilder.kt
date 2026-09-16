package com.teamyg.parfait.feature.groups.canvas.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasBGEdit
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSave
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasBGEditRoute
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasImageSaveRoute
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasMainRoute
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasToppingPlace
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasToppingPlaceRoute

fun EntryProviderScope<NavKey>.featureCanvasEntryBuilder(navigator: Navigator) {
    entry<NavKeyCanvasMain> { navKey ->
        CanvasMainRoute(
            navKey = navKey,
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }

    // 배경 편집은 실패를 토스트로 알려 자기 Scaffold(YGScaffoldV2)를 직접 든다 —
    // 여기서 한 겹 더 씌우면 인셋 패딩이 두 번 먹는다
    entry<NavKeyCanvasBGEdit> { navKey ->
        CanvasBGEditRoute(
            groupId = navKey.groupId,
            parfaitId = navKey.parfaitId,
            initialToppingId = navKey.initialToppingId,
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }

    entry<NavKeyCanvasImageSave> { navKey ->
        CanvasImageSaveRoute(
            navKey = navKey,
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }

    entry<NavKeyCanvasToppingPlace> {
        CanvasToppingPlaceRoute(
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
