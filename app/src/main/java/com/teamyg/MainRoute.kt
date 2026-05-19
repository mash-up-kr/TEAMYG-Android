package com.teamyg

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.feature.sample.UserScreen

@Composable
fun MainRoute(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        UserScreen()
    }
}
