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
import com.teamyg.parfait.core.navigation.NavTransition
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMove
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasToppingPlace
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasMoveRoute
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasToppingPlaceRoute

fun EntryProviderScope<NavKey>.featureCanvasEntryBuilder(navigator: Navigator) {
    entry<NavKeyCanvasMain> { navKey ->
        CanvasMainRoute(
            groupId = navKey.groupId,
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
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }
    // 이 화면과 이미지 선택 화면은 사진 하나를 공유 요소로 이어 붙인다. 화면 전체가 옆으로
    // 밀리면 정작 봐야 할 사진의 이동이 묻히므로 여기서만 제자리 전환으로 바꾼다
    entry<NavKeyCanvasEdit>(metadata = NavTransition.Fade.metadata) { navKey ->
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

    entry<NavKeyCanvasToppingPlace> {
        CanvasToppingPlaceRoute(
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
