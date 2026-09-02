package com.teamyg.parfait.feature.camera.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.camera.api.NavKeyCameraSystem
import com.teamyg.parfait.feature.camera.api.NavKeyPictureConfirm
import com.teamyg.parfait.feature.camera.impl.route.CustomCameraRoute
import com.teamyg.parfait.feature.camera.impl.route.PictureConfirmRoute
import com.teamyg.parfait.feature.camera.impl.route.SystemCameraRoute

fun EntryProviderScope<NavKey>.featureCameraEntryBuilder(navigator: Navigator) {
    entry<NavKeyCameraCustom> { navKey ->
        CustomCameraRoute(
            navigator = navigator,
            showGuideToast = navKey.showGuideToast,
            returnResultOnly = navKey.returnResultOnly,
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<NavKeyCameraSystem> {
        SystemCameraRoute(
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<NavKeyPictureConfirm> { navKey ->
        PictureConfirmRoute(
            uri = navKey.uri,
            source = navKey.source,
            returnResultOnly = navKey.returnResultOnly,
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
