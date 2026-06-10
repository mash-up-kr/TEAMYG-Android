package com.teamyg.canvas.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.teamyg.canvas.impl.R
import com.tjyg.core.ui.LocalSharedTransitionScope

@Composable
internal fun CanvasImageSelectScreen(
    onClickImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopEnd,
    ) {
        with(sharedTransitionScope) {
            Image(
                painter = painterResource(id = R.drawable.nukkiii),
                contentDescription = null,
                modifier = Modifier
                    .sharedElement(
                        rememberSharedContentState(key = "parfait"),
                        animatedVisibilityScope = animatedContentScope,
                    ).size(100.dp)
                    .clickable { onClickImage() },
            )
        }
    }
}
