package com.teamyg.parfait.core.designsystem.theme.colors

import androidx.compose.ui.graphics.Color

internal object YGSemanticColorDefaults {
    private val DefaultYGGrayScale: YGColorGrayScale = YGColorGrayScale(
        transparent = YGAtomicColors.Gray.Transparent,
        white = YGAtomicColors.Gray.White,
        black = YGAtomicColors.Gray.Black,
    )

    internal val YGLightColorScheme: YGColorScheme = YGColorScheme(
        primary = YGAtomicColors.Cherry.Cherry,
        secondary = YGAtomicColors.Melon.Melon,
        tertiary = YGAtomicColors.Pudding.Pudding,
        danger = Color(0xFFFF3A3D), // TODO Atomic 적용 필요
        warning = Color(0xFFFFFF32), // TODO Atomic 적용 필요
        success = Color(0xFF2AE6AE), // TODO Atomic 적용 필요
        info = Color(0xFF2B9BE7), // TODO Atomic 적용 필요
        grayScale = DefaultYGGrayScale,
    )

    // TODO 나중에 다크모드 추가할거면 변경 필요
    internal val YGDarkColorScheme: YGColorScheme = YGLightColorScheme
}
