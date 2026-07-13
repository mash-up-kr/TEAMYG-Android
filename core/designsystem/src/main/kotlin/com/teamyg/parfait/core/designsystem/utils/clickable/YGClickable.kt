package com.teamyg.parfait.core.designsystem.utils.clickable

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

fun Modifier.clickableYG(
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = ygDimRipple(),
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    windowMillis: Long = 300L,
    onClick: () -> Unit,
): Modifier = this then ClickableYGElement(
    interactionSource = interactionSource,
    indication = indication,
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    windowMillis = windowMillis,
    onClick = onClick,
)

private data class ClickableYGElement(
    val interactionSource: MutableInteractionSource?,
    val indication: Indication?,
    val enabled: Boolean,
    val onClickLabel: String?,
    val role: Role?,
    val windowMillis: Long,
    val onClick: () -> Unit,
) : ModifierNodeElement<ClickableYGNode>() {
    override fun create(): ClickableYGNode = ClickableYGNode(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        windowMillis = windowMillis,
        onClick = onClick,
    )

    override fun update(node: ClickableYGNode) {
        node.update(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            windowMillis = windowMillis,
            onClick = onClick,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "clickableYG"
        properties["enabled"] = enabled
        properties["windowMillis"] = windowMillis
        properties["role"] = role
        properties["onClickLabel"] = onClickLabel
    }
}

private class ClickableYGNode(
    private var interactionSource: MutableInteractionSource?,
    private var indication: Indication?,
    private var enabled: Boolean,
    private var onClickLabel: String?,
    private var role: Role?,
    private var windowMillis: Long,
    private var onClick: () -> Unit,
) : DelegatingNode(), SemanticsModifierNode {
    private var lastMark: TimeSource.Monotonic.ValueTimeMark? = null

    private var ownSource: MutableInteractionSource? = null
    private val source: MutableInteractionSource
        get() = interactionSource ?: ownSource ?: MutableInteractionSource().also { ownSource = it }

    private var indicationNode: DelegatableNode? = null

    init {
        delegate(
            SuspendingPointerInputModifierNode {
                detectTapGestures(
                    onPress = { offset ->
                        if (!enabled) {
                            return@detectTapGestures
                        }

                        val press = PressInteraction.Press(offset)
                        source.emit(press)

                        val released = tryAwaitRelease()
                        source.emit(
                            if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                        )
                    },
                    onTap = { performClick() },
                )
            },
        )
    }

    override fun onAttach() {
        attachIndication()
    }

    private fun attachIndication() {
        val current = indicationNode
        if (current != null) {
            undelegate(current)
            indicationNode = null
        }
        val ind = indication
        if (ind is IndicationNodeFactory) {
            indicationNode = delegate(ind.create(source))
        }
    }

    private fun performClick() {
        if (!enabled) {
            return
        }

        val mark = lastMark

        if (mark == null || mark.elapsedNow() >= windowMillis.milliseconds) {
            lastMark = TimeSource.Monotonic.markNow()
            onClick()
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        this@ClickableYGNode.role?.let { role = it }

        onClick(label = onClickLabel) {
            performClick()
            true
        }

        if (!enabled) {
            disabled()
        }
    }

    fun update(
        interactionSource: MutableInteractionSource?,
        indication: Indication?,
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        windowMillis: Long,
        onClick: () -> Unit,
    ) {
        val indicationChanged = this.indication != indication || this.interactionSource != interactionSource
        val semanticsChanged = this.enabled != enabled || this.role != role || this.onClickLabel != onClickLabel

        this.interactionSource = interactionSource
        this.indication = indication
        this.enabled = enabled
        this.onClickLabel = onClickLabel
        this.role = role
        this.windowMillis = windowMillis
        this.onClick = onClick

        if (indicationChanged) {
            attachIndication()
        }
        if (semanticsChanged) {
            invalidateSemantics()
        }
    }
}
