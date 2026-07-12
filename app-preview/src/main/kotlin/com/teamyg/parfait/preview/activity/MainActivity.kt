package com.teamyg.parfait.preview.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.preview.route.RootRoute
import com.teamyg.parfait.preview.navigation.key.NavKeyMain
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var entryBuilders: Set<@JvmSuppressWildcards EntryProviderScope<NavKey>.(Navigator) -> Unit>

    private val navigator: Navigator = Navigator(NavKeyMain)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YGCustomTheme {
                RootRoute(
                    navigator = navigator,
                    entryBuilders = entryBuilders,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
