package com.teamyg.parfait.core.util.android.clickable

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.createRippleModifierNode
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.unit.Dp

val YGDimRippleAlpha: RippleAlpha = RippleAlpha(
    pressedAlpha = 0.15f,
    focusedAlpha = 0.15f,
    draggedAlpha = 0.15f,
    hoveredAlpha = 0.15f,
)

val YGDimRippleColor: Color = Color(0xFF29292C)

@Stable
fun ygDimRipple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = YGDimRippleColor,
    rippleAlpha: RippleAlpha = YGDimRippleAlpha,
): IndicationNodeFactory = YGDimRippleNodeFactory(
    bounded = bounded,
    radius = radius,
    color = color,
    rippleAlpha = rippleAlpha,
)

internal class YGDimRippleNodeFactory(
    private val bounded: Boolean,
    private val radius: Dp,
    private val color: Color,
    private val rippleAlpha: RippleAlpha,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = DelegatingYGDimRippleNode(
        interactionSource = interactionSource,
        bounded = bounded,
        radius = radius,
        color = color,
        rippleAlpha = rippleAlpha,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is YGDimRippleNodeFactory) {
            return false
        }
        if (bounded != other.bounded) {
            return false
        }
        if (radius != other.radius) {
            return false
        }
        if (color != other.color) {
            return false
        }
        return rippleAlpha == other.rippleAlpha
    }

    override fun hashCode(): Int {
        var result = bounded.hashCode()
        result = 31 * result + radius.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + rippleAlpha.hashCode()
        return result
    }
}

private class DelegatingYGDimRippleNode(
    private val interactionSource: InteractionSource,
    private val bounded: Boolean,
    private val radius: Dp,
    private val color: Color,
    private val rippleAlpha: RippleAlpha,
) : DelegatingNode(), CompositionLocalConsumerModifierNode {
    override fun onAttach() {
        delegate(
            createRippleModifierNode(
                interactionSource = interactionSource,
                bounded = bounded,
                radius = radius,
                color = { color },
                rippleAlpha = { rippleAlpha },
            ),
        )
    }
}
