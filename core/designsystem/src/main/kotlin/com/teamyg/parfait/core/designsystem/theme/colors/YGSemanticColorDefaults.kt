package com.teamyg.parfait.core.designsystem.theme.colors

internal object YGSemanticColorDefaults {
    private val DefaultYGGrayScale: YGColorGrayScale = YGColorGrayScale(
        transparent = YGAtomicColors.Gray.Transparent,
        white = YGAtomicColors.Gray.White,
        black = YGAtomicColors.Gray.Black,
    )

    private val DefaultYGTransparency: YGColorTransparency = YGColorTransparency(
        white25 = YGAtomicColors.Transparency.White25,
        white50 = YGAtomicColors.Transparency.White50,
        white75 = YGAtomicColors.Transparency.White75,
        black5 = YGAtomicColors.Transparency.Black5,
        black25 = YGAtomicColors.Transparency.Black25,
        black50 = YGAtomicColors.Transparency.Black50,
        black75 = YGAtomicColors.Transparency.Black75,
    )

    internal val YGLightColorScheme: YGColorScheme = YGColorScheme(
        primary = YGAtomicColors.Cherry.Cherry,
        secondary = YGAtomicColors.Melon.Melon500,
        tertiary = YGAtomicColors.Pudding.Pudding500,
        danger = YGAtomicColors.Cherry.Cherry600,
        warning = YGAtomicColors.Pudding.Pudding600,
        success = YGAtomicColors.Melon.Melon600,
        info = YGAtomicColors.Soda.Soda500,
        grayScale = DefaultYGGrayScale,
        transparency = DefaultYGTransparency,
    )

    // TODO 나중에 다크모드 추가할거면 변경 필요.
    //  시스템바 아이콘도 같이 열어야 한다(parfait/adr/0028-system-bar-light-fixed.md).
    internal val YGDarkColorScheme: YGColorScheme = YGLightColorScheme
}
