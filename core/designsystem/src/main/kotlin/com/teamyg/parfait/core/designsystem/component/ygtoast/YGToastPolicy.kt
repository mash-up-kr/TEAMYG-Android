package com.teamyg.parfait.core.designsystem.component.ygtoast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock

private const val DISMISS_DELAY = 2000L
private const val ANIMATION_DURATION = 300
private const val SWIPE_DISMISS_THRESHOLD = -100f
private const val FLING_DISMISS_VELOCITY = -500f

data class YGToast(
    val id: String,
    val type: YGToastType,
    val visible: Boolean = true,
)

@Stable
class YGToastPolicy {
    val toasts = mutableStateListOf<YGToast>()

    fun show(type: YGToastType) {
        toasts.add(
            0,
            YGToast(
                id = Clock.System.now().toEpochMilliseconds().toString(),
                type = type,
            ),
        )
    }

    internal fun setVisible(
        id: String,
        visible: Boolean,
    ) {
        val idx = toasts.indexOfFirst { it.id == id }
        if (idx != -1) toasts[idx] = toasts[idx].copy(visible = visible)
    }

    internal fun removeToast(id: String) {
        toasts.removeAll { it.id == id }
    }
}

@Composable
fun rememberYGToastPolicy(): YGToastPolicy = remember { YGToastPolicy() }

@Composable
fun YGToastHost(
    policy: YGToastPolicy,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier,
    ) {
        policy.toasts.forEach { toast ->
            key(toast.id) {
                var dragOffsetY by remember { mutableStateOf(0f) }

                LaunchedEffect(toast.id) {
                    delay(DISMISS_DELAY)
                    policy.setVisible(toast.id, false)
                    policy.removeToast(toast.id)
                }

                AnimatedVisibility(
                    visible = toast.visible,
                    enter = slideInVertically(tween(ANIMATION_DURATION)) { -it },
                    exit = slideOutVertically(tween(ANIMATION_DURATION)) { -it },
                ) {
                    YGToast(
                        type = toast.type,
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
                                            policy.setVisible(toast.id, false)
                                            policy.removeToast(toast.id)
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
}
