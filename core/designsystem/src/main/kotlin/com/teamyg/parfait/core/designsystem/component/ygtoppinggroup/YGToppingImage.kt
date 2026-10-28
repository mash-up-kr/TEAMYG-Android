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
    @Immutable
    data class Remote(
        val url: String,
        val border: YGToppingBorder? = null,
    ) : YGToppingImage

    @Immutable
    data class Template(val type: YGToppingTemplate) : YGToppingImage

    /** 마지막 토핑이 텍스트일 때. 다른 이미지와 달리 [YGToppingGroupType.rotation] 을 받지 않는다 */
    @Immutable
    data class TextTemplate(val type: YGToppingTextTemplate) : YGToppingImage

    @Immutable
    data class Local(@DrawableRes val drawableRes: Int) : YGToppingImage

    data object Error : YGToppingImage
}
