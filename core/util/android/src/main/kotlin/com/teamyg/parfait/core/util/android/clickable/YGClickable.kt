package com.teamyg.parfait.core.util.android.clickable

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.semantics.Role
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

@Composable
fun Modifier.clickableYGNoRipple(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    windowMillis: Long = 300L,
    onClick: () -> Unit,
): Modifier = clickableYGThrottle(
    interactionSource = interactionSource,
    indications = null,
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    windowMillis = windowMillis,
    onClick = onClick,
)

@Composable
fun Modifier.clickableYG(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    windowMillis: Long = 300L,
    onClick: () -> Unit,
): Modifier = clickableYGDimRipple(
    interactionSource = interactionSource,
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    windowMillis = windowMillis,
    onClick = onClick,
)

@Composable
fun Modifier.clickableYGDimRipple(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    windowMillis: Long = 300L,
    onClick: () -> Unit,
): Modifier = clickableYGThrottle(
    interactionSource = interactionSource,
    indications = listOf(ygDimRipple()),
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    windowMillis = windowMillis,
    onClick = onClick,
)

@Composable
fun Modifier.clickableYGScaleRipple(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    windowMillis: Long = 300L,
    onClick: () -> Unit,
): Modifier = clickableYGThrottle(
    interactionSource = interactionSource,
    indications = listOf(ygScaleRipple()),
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    windowMillis = windowMillis,
    onClick = onClick,
)

@Composable
fun Modifier.clickableYGMergeRipple(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    windowMillis: Long = 300L,
    onClick: () -> Unit,
): Modifier = clickableYGThrottle(
    interactionSource = interactionSource,
    indications = listOf(ygDimRipple(), ygScaleRipple()),
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    windowMillis = windowMillis,
    onClick = onClick,
)

@Composable
internal fun Modifier.clickableYGThrottle(
    indications: List<Indication>?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    windowMillis: Long = 300L,
    interactionSource: MutableInteractionSource? = remember { MutableInteractionSource() },
    onClick: () -> Unit,
): Modifier {
    val gate = remember { YGClickThrottleGate() }
    val indication = remember(indications) { indications?.toYGIndication() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
    ) {
        if (gate.tryPass(windowMillis)) {
            onClick()
        }
    }
}

private class YGClickThrottleGate {
    private var lastMark: TimeSource.Monotonic.ValueTimeMark? = null

    fun tryPass(windowMillis: Long): Boolean {
        val mark = lastMark
        return if (mark == null || mark.elapsedNow() >= windowMillis.milliseconds) {
            lastMark = TimeSource.Monotonic.markNow()
            true
        } else {
            false
        }
    }
}

/** 리플 목록을 단일 [Indication]으로 접는다. 다중은 자식을 delegate하는 합성 팩토리. */
private fun List<Indication>.toYGIndication(): Indication? = when {
    isEmpty() -> null
    size == 1 -> single()
    else -> YGCompositeIndicationNodeFactory(filterIsInstance<IndicationNodeFactory>())
}

internal class YGCompositeIndicationNodeFactory(
    private val factories: List<IndicationNodeFactory>,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        YGCompositeIndicationNode(interactionSource, factories)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is YGCompositeIndicationNodeFactory) {
            return false
        }
        return factories == other.factories
    }

    override fun hashCode(): Int = factories.hashCode()
}

private class YGCompositeIndicationNode(
    private val interactionSource: InteractionSource,
    private val factories: List<IndicationNodeFactory>,
) : DelegatingNode() {
    override fun onAttach() {
        factories.forEach { factory ->
            delegate(factory.create(interactionSource))
        }
    }
}
