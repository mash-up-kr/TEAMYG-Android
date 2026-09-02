package com.teamyg.parfait.core.designsystem.component.ygtoast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

private const val DISMISS_DELAY = 2000L
private const val ANIMATION_DURATION = 300
private const val SWIPE_DISMISS_THRESHOLD = -100f
private const val FLING_DISMISS_VELOCITY = -500f

data class YGToastItem(
    val id: String,
    val type: YGToastType,
    val visible: Boolean = true,
    val tag: String? = null,
)

@Stable
class YGToastPolicy {
    val toasts = mutableStateListOf<YGToastItem>()

    /**
     * @param replaceTag 같은 태그가 떠 있으면 닫고 이것만 남긴다. 주지 않으면 쌓인다.
     *   [YGToastType] 으로 나누지 않는 이유는 같은 타입이라도 쌓여야 하는 토스트가 있어서다.
     */
    fun show(
        type: YGToastType,
        replaceTag: String? = null,
    ) {
        if (replaceTag != null) toasts.removeAll { it.tag == replaceTag }

        toasts.add(
            0,
            YGToastItem(
                id = UUID.randomUUID().toString(),
                type = type,
                tag = replaceTag,
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

/**
 * 실패를 알리는 토스트를 띄운다. 공통 실패 표현은 [YGToastType.Fail] 하나로 고정이고,
 * 문구는 호출부가 만든다 — 실패의 어휘가 화면마다 다르기 때문이다(로그인은 카카오 로그인
 * 실패, 갤러리는 저장 실패).
 *
 * 재시도 동선이 필요한 실패는 이걸 쓰지 않는다. 그런 실패는 화면이 자기 UI 로 표현한다.
 */
fun YGToastPolicy.showError(text: String) {
    show(YGToastType.Fail(text))
}

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

@YGPreview
@Composable
private fun YGToastHostPreview() = PreviewBox {
    val previewTypes = listOf(
        YGToastType.Record(userName = "WWWWWWWWWW", time = "59분 전", userNameColor = YGAtomicColors.Pudding.Pudding500),
        YGToastType.Edit("내 토핑만 편집할 수 있어요"),
        YGToastType.InviteCode("초대 코드를 복사했어요"),
        YGToastType.Fail("갤러리 저장에 실패했어요. 나중에 다시 시도해 주세요."),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        previewTypes.forEach { type ->
            val policy = remember(type) {
                YGToastPolicy().apply { show(type) }
            }
            YGToastHost(
                policy = policy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
