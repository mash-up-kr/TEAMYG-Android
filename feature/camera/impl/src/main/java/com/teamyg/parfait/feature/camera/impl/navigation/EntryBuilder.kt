package com.teamyg.parfait.feature.camera.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.camera.api.NavKeyCameraSystem
import com.teamyg.navigation.Navigator
import com.teamyg.parfait.feature.camera.impl.route.CustomCameraRoute
import com.teamyg.parfait.feature.camera.impl.route.SystemCameraRoute

fun EntryProviderScope<NavKey>.featureCameraEntryBuilder(navigator: Navigator) {
    entry<NavKeyCameraCustom> {
        Scaffold { innerPadding ->
            CustomCameraRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
    entry<NavKeyCameraSystem> {
        Scaffold { innerPadding ->
            SystemCameraRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
