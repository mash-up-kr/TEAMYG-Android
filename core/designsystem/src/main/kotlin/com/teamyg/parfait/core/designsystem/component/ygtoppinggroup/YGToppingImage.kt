package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import androidx.compose.runtime.Immutable
import com.teamyg.parfait.core.designsystem.R

internal val TOPPING_ERROR_DRAWABLE: Int = R.drawable.img_topping_template_error

/**
 * YGToppingGroup Image Type
 */
@Immutable
sealed interface YGToppingImage {
    @Immutable
    data class Remote(val url: String) : YGToppingImage

    @Immutable
    data class Template(val type: YGToppingTemplate) : YGToppingImage

    data object Error : YGToppingImage
}
