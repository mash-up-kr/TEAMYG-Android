package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget

/**
 * 테두리 패널이 있는 캔버스의 입력 셋을 정해진 순서로 단다. 패널이 열려 있으면 터치는 패널만 닫는다.
 *
 * 세 입력의 순서와, 패널 상태로 체인을 갈아 끼우지 않는 이유는 [dismissPanelOnTouch] KDoc 참고.
 * 화면은 셋을 따로 달지 않고 이것만 부른다.
 *
 * @param isPanelOpen 호출 시점의 상태를 읽는 람다여야 한다. 값을 미리 읽어 넘기면 탭·변형 입력이
 *   닫힌 뒤의 상태를 못 본다.
 */
@Composable
internal fun <T> Modifier.toppingPanelInputs(
    isPanelOpen: () -> Boolean,
    onDismissPanel: () -> Unit,
    tapEntries: () -> List<Pair<T, ToppingHitTarget>>,
    tapKeyOf: (T) -> Any,
    onTapHit: (T) -> Unit,
    onTapMiss: () -> Unit,
    transformTargetAt: () -> ToppingHitTarget?,
    onTransform: (pan: Offset, zoom: Float, rotationDelta: Float) -> Unit,
    onGestureActiveChange: (Boolean) -> Unit = {},
): Modifier = this
    .dismissPanelOnTouch(
        isPanelOpen = isPanelOpen,
        onDismiss = onDismissPanel,
    ).toppingTapInput(
        entries = tapEntries,
        keyOf = tapKeyOf,
        onHit = onTapHit,
        onMiss = onTapMiss,
        enabled = { !isPanelOpen() },
    ).toppingTransformInput(
        targetAt = transformTargetAt,
        onTransform = onTransform,
        onGestureActiveChange = onGestureActiveChange,
        enabled = { !isPanelOpen() },
    )
