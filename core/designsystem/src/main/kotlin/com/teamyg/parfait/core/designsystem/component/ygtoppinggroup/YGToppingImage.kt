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

    /** 디자인 시스템 밖 모듈이 가진 drawable 을 넘기는 길이다. */
    @Immutable
    data class Local(@DrawableRes val drawableRes: Int) : YGToppingImage

    data object Error : YGToppingImage
}
