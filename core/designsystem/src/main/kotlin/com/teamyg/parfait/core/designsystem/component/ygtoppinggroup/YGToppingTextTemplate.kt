package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import androidx.annotation.DrawableRes
import com.teamyg.parfait.core.designsystem.R

/** 텍스트 토핑 전용이라 [YGToppingTemplate] 과 섞어 고르지 않도록 따로 둔다. */
enum class YGToppingTextTemplate(@DrawableRes val drawableRes: Int) {
    TEMPLATE_01(R.drawable.img_topping_template_text_01),
    TEMPLATE_02(R.drawable.img_topping_template_text_02),
}
