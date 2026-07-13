package com.teamyg.parfait.core.util.android.clickable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch

@Stable
fun ygScaleRipple(
    scaleEnabled: Boolean = true,
    scaleValue: Float = 0.98f,
): IndicationNodeFactory = YGScaleNodeFactory(
    scaleEnabled = scaleEnabled,
    scaleValue = scaleValue,
)

internal class YGScaleNodeFactory(
    private val scaleEnabled: Boolean,
    private val scaleValue: Float,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = DelegatingYGScaleRippleNode(
        interactionSource = interactionSource,
        scaleEnabled = scaleEnabled,
        scaleValue = scaleValue,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is YGScaleNodeFactory) {
            return false
        }
        if (scaleEnabled != other.scaleEnabled) {
            return false
        }
        return scaleValue == other.scaleValue
    }

    override fun hashCode(): Int {
        var result = scaleEnabled.hashCode()
        result = 31 * result + scaleValue.hashCode()
        return result
    }
}

private class DelegatingYGScaleRippleNode(
    private val interactionSource: InteractionSource,
    private val scaleEnabled: Boolean,
    private val scaleValue: Float,
) : DelegatingNode(), DrawModifierNode {
    private val animatable = Animatable(1f)

    override fun onAttach() {
        if (!scaleEnabled) {
            return
        }
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        animatable.animateTo(
                            targetValue = scaleValue,
                            animationSpec = tween(
                                durationMillis = 150,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    }

                    is PressInteraction.Release,
                    is PressInteraction.Cancel,
                    -> {
                        animatable.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        )
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        if (scaleEnabled) {
            val scale = animatable.value
            scale(scale, scale) {
                this@draw.drawContent()
            }
        } else {
            drawContent()
        }
    }
}
