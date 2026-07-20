package com.teamyg.parfait.preview.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.preview.screen.MainScreen

@Composable
internal fun MainRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val toastPolicy = rememberYGToastPolicy()
    MainScreen(
        toast = toastPolicy,
        modifier = modifier,
    )
}
