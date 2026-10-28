package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.teamyg.parfait.core.designsystem.R

internal val TOPPING_ERROR_DRAWABLE: Int = R.drawable.img_topping_template_error

/**
 * YGToppingGroup Image Type
 */
@Immutable
sealed interface YGToppingImage {
    val followsCardRotation: Boolean get() = true

    @Immutable
    data class Remote(
        val url: String,
        val border: YGToppingBorder? = null,
    ) : YGToppingImage

    @Immutable
    data class Template(val type: YGToppingTemplate) : YGToppingImage

    /** 글자 표시가 기울면 읽기 어려워 카드 기울기를 따르지 않는다 */
    @Immutable
    data class TextTemplate(val type: YGToppingTextTemplate) : YGToppingImage {
        override val followsCardRotation: Boolean get() = false
    }

    @Immutable
    data class Local(@DrawableRes val drawableRes: Int) : YGToppingImage

    data object Error : YGToppingImage
}
