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

internal fun Modifier.clickableYGThrottle(
    interactionSource: MutableInteractionSource? = null,
    indications: List<Indication>,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    windowMillis: Long = 300L,
    onClick: () -> Unit,
): Modifier = this then ClickableYGElement(
    interactionSource = interactionSource,
    indications = indications,
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    windowMillis = windowMillis,
    onClick = onClick,
)

private class ClickableYGElement(
    val interactionSource: MutableInteractionSource?,
    val indications: List<Indication>,
    val enabled: Boolean,
    val onClickLabel: String?,
    val role: Role?,
    val windowMillis: Long,
    val onClick: () -> Unit,
) : ModifierNodeElement<ClickableYGNode>() {
    override fun create(): ClickableYGNode = ClickableYGNode(
        interactionSource = interactionSource,
        indications = indications,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        windowMillis = windowMillis,
        onClick = onClick,
    )

    override fun update(node: ClickableYGNode) {
        node.update(
            interactionSource = interactionSource,
            indications = indications,
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

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ClickableYGElement) {
            return false
        }
        if (enabled != other.enabled) {
            return false
        }
        if (windowMillis != other.windowMillis) {
            return false
        }
        if (onClickLabel != other.onClickLabel) {
            return false
        }
        if (role != other.role) {
            return false
        }
        if (interactionSource != other.interactionSource) {
            return false
        }
        if (indications != other.indications) {
            return false
        }
        if (onClick !== other.onClick) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = interactionSource?.hashCode() ?: 0
        result = 31 * result + indications.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (onClickLabel?.hashCode() ?: 0)
        result = 31 * result + (role?.hashCode() ?: 0)
        result = 31 * result + windowMillis.hashCode()
        result = 31 * result + onClick.hashCode()
        return result
    }
}

private class ClickableYGNode(
    private var interactionSource: MutableInteractionSource?,
    private var indications: List<Indication>,
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

    private val indicationNodes = mutableListOf<DelegatableNode>()

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
        attachIndications()
    }

    private fun attachIndications() {
        indicationNodes.forEach { undelegate(it) }
        indicationNodes.clear()
        indications.forEach { indication ->
            if (indication is IndicationNodeFactory) {
                indicationNodes += delegate(indication.create(source))
            }
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
        indications: List<Indication>,
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        windowMillis: Long,
        onClick: () -> Unit,
    ) {
        val indicationsChanged = this.indications != indications || this.interactionSource != interactionSource
        val semanticsChanged = this.enabled != enabled || this.role != role || this.onClickLabel != onClickLabel

        this.interactionSource = interactionSource
        this.indications = indications
        this.enabled = enabled
        this.onClickLabel = onClickLabel
        this.role = role
        this.windowMillis = windowMillis
        this.onClick = onClick

        if (indicationsChanged) {
            attachIndications()
        }
        if (semanticsChanged) {
            invalidateSemantics()
        }
    }
}
