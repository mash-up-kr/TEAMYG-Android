package com.teamyg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.analytics.AnalyticsHelper
import com.teamyg.designsystem.theme.TempYGMaterialTheme
import com.teamyg.model.qualifier.FeatureQualifier
import com.teamyg.navigation.Navigator
import com.tjyg.core.ui.local.LocalAnalyticsHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var entryBuilders: Set<@JvmSuppressWildcards EntryProviderScope<NavKey>.(Navigator) -> Unit>

    @Inject
    lateinit var navigator: Navigator

    @Inject
    @FeatureQualifier
    lateinit var analyticsHelper: AnalyticsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        analyticsHelper.d { "entry MainActivity onCreate" }

        setContent {
            TempYGMaterialTheme {
                CompositionLocalProvider(
                    LocalAnalyticsHelper provides analyticsHelper,
                ) {
                    MainRoute(
                        navigator = navigator,
                        entryBuilders = entryBuilders,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
