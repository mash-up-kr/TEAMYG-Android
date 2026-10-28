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
    /** 카드 배치([YGToppingGroupType.rotation])의 기울기를 따를지 */
    val followsCardRotation: Boolean get() = true

    @Immutable
    data class Remote(
        val url: String,
        val border: YGToppingBorder? = null,
    ) : YGToppingImage

    @Immutable
    data class Template(val type: YGToppingTemplate) : YGToppingImage

    /** 마지막 토핑이 텍스트일 때. 말풍선 속 글자 표시가 기울면 읽기 어려워 카드 기울기를 따르지 않는다 */
    @Immutable
    data class TextTemplate(val type: YGToppingTextTemplate) : YGToppingImage {
        override val followsCardRotation: Boolean get() = false
    }

    @Immutable
    data class Local(@DrawableRes val drawableRes: Int) : YGToppingImage

    data object Error : YGToppingImage
}
