package com.teamyg.parfait.host

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYG
import com.teamyg.parfait.viewmodel.MainState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun GeneralLoadingHost(state: MainState.LoadingState) {
    var animationVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoading) {
        animationVisible = state.isLoading

        if (state.isLoading) {
            delay(10.seconds)
            animationVisible = false
        }
    }

    AnimatedVisibility(
        visible = animationVisible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(animationSpec = tween(delayMillis = 100)),
        exit = fadeOut(animationSpec = tween(delayMillis = 100)),
    ) {
        BackHandler {
            // Not impl
        }

        Box(
            modifier = Modifier.fillMaxSize()
                .clickableYG {}
                .background(
                    color = YGAtomicColors.Gray.White.copy(alpha = 0.8f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = YGAtomicColors.Cherry.Cherry,
                modifier = Modifier.size(120.dp),
                strokeWidth = 6.dp,
                trackColor = YGAtomicColors.Gray.Gray300,
            )
        }
    }
}

@YGPreview
@Composable
private fun PreviewGeneralLoadingHost() = PreviewBox {
    GeneralLoadingHost(
        state = MainState.LoadingState(isLoading = true),
    )
}
