package com.teamyg.parfait.core.designsystem.component.ygalert

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

private const val DISMISS_DELAY = 2500L
private const val ANIMATION_DURATION = 300
private const val SWIPE_DISMISS_THRESHOLD = -100f
private const val FLING_DISMISS_VELOCITY = -500f

data class YGAlertItem(
    val id: String,
    val title: String,
    val sub: String,
    val buttonText: String? = null,
    val onButtonClick: (() -> Unit)? = null,
    @DrawableRes val buttonIconResource: Int = R.drawable.ic_caret_right,
    val visible: Boolean = true,
)

@Stable
class YGAlertPolicy {
    var alert by mutableStateOf<YGAlertItem?>(null)

    fun show(
        title: String,
        sub: String,
        buttonText: String? = null,
        onButtonClick: (() -> Unit)? = null,
        @DrawableRes buttonIconResource: Int = R.drawable.ic_caret_right,
    ) {
        alert = YGAlertItem(
            id = UUID.randomUUID().toString(),
            title = title,
            sub = sub,
            buttonText = buttonText,
            onButtonClick = onButtonClick,
            buttonIconResource = buttonIconResource,
        )
    }

    internal fun clearAlert() {
        alert = null
    }
}

@Composable
fun rememberYGAlertPolicy(): YGAlertPolicy = remember { YGAlertPolicy() }

@Composable
fun YGAlertHost(
    policy: YGAlertPolicy,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    Box(modifier = modifier) {
        policy.alert?.let { alert ->
            LaunchedEffect(alert.id) {
                delay(DISMISS_DELAY)
                policy.clearAlert()
            }

            AnimatedVisibility(
                visible = alert.visible,
                enter = slideInVertically(tween(ANIMATION_DURATION)) { -it },
                exit = slideOutVertically(tween(ANIMATION_DURATION)) { -it },
            ) {
                var dragOffsetY by remember { mutableStateOf(0f) }
                YGAlert(
                    title = alert.title,
                    sub = alert.sub,
                    buttonText = alert.buttonText,
                    onButtonClick = alert.onButtonClick,
                    buttonIconResource = alert.buttonIconResource,
                    modifier = Modifier
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                if (delta < 0) dragOffsetY += delta
                            },
                            onDragStopped = { velocity ->
                                if (dragOffsetY < SWIPE_DISMISS_THRESHOLD || velocity < FLING_DISMISS_VELOCITY) {
                                    dragOffsetY = 0f
                                    scope.launch {
                                        policy.clearAlert()
                                    }
                                } else {
                                    dragOffsetY = 0f
                                }
                            },
                        ).offset { IntOffset(0, dragOffsetY.toInt()) },
                )
            }
        }
    }
}

@YGPreview
@Composable
private fun YGAlertHostPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val withButtonPolicy = remember {
            YGAlertPolicy().apply {
                show(
                    title = "Title",
                    sub = "Sub",
                    buttonText = "Text",
                    onButtonClick = {},
                )
            }
        }
        val textOnlyPolicy = remember {
            YGAlertPolicy().apply {
                show(
                    title = "Title",
                    sub = "Sub",
                )
            }
        }

        YGAlertHost(
            policy = withButtonPolicy,
            modifier = Modifier.fillMaxWidth(),
        )
        YGAlertHost(
            policy = textOnlyPolicy,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
