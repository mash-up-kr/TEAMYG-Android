package com.teamyg.parfait.feature.camera.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.camera.api.NavKeyCameraSystem
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyPictureConfirm
import com.teamyg.parfait.feature.camera.impl.route.CustomCameraRoute
import com.teamyg.parfait.feature.camera.impl.route.PictureConfirmRoute
import com.teamyg.parfait.feature.camera.impl.route.SystemCameraRoute

fun EntryProviderScope<NavKey>.featureCameraEntryBuilder(navigator: Navigator) {
    entry<NavKeyCameraCustom> { navKey ->
        // 카메라 피드는 시스템 바 아래까지 덮어야 하므로 innerPadding을 화면에 먹이지 않는다.
        // 인셋은 CustomCameraScreen의 컨트롤 영역이 직접 처리한다.
        YGScaffold {
            CustomCameraRoute(
                navigator = navigator,
                showGuideToast = navKey.showGuideToast,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    entry<NavKeyCameraSystem> {
        YGScaffold { innerPadding ->
            SystemCameraRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
    entry<NavKeyPictureConfirm> { navKey ->
        YGScaffold { innerPadding ->
            PictureConfirmRoute(
                uri = navKey.uri,
                source = navKey.source,
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
