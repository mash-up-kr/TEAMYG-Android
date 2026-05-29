package com.teamyg.camera.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.camera.api.NavKeyCameraSystem
import com.teamyg.camera.impl.route.SystemCameraRoute
import com.teamyg.navigation.Navigator

fun EntryProviderScope<NavKey>.featureCameraEntryBuilder(navigator: Navigator) {
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
