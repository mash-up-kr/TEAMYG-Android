package com.teamyg.canvas.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.teamyg.canvas.impl.R
import com.tjyg.core.ui.LocalSharedTransitionScope

@Composable
fun CanvasEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(0xffc9c9c9)),
    ) {
        with(sharedTransitionScope) {
            Image(
                painter = painterResource(id = R.drawable.nukkiii),
                contentDescription = null,
                modifier = Modifier
                    .sharedElement(
                        rememberSharedContentState(key = "parfait"),
                        animatedVisibilityScope = animatedContentScope,
                    ).size(200.dp)
                    .align(Alignment.Center),
            )
        }
        Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Text("완료")
        }
    }
}
