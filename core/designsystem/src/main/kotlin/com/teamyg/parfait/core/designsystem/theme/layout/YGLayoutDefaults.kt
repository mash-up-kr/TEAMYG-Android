package com.teamyg.parfait.core.designsystem.theme.layout

import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

internal object YGLayoutDefaults {
    private val YGDefaultLayoutGap: YGLayoutGap = YGLayoutGap(
        gap1 = SizeTokens.Size2.getDp(),
        gap2 = SizeTokens.Size4.getDp(),
        gap3 = SizeTokens.Size8.getDp(),
        gap4 = SizeTokens.Size12.getDp(),
        gap5 = SizeTokens.Size16.getDp(),
        gap6 = SizeTokens.Size20.getDp(),
        gap7 = SizeTokens.Size24.getDp(),
        gap8 = SizeTokens.Size32.getDp(),
        gap9 = SizeTokens.Size40.getDp(),
        gap10 = SizeTokens.Size48.getDp(),
        gap11 = SizeTokens.Size64.getDp(),
        gap12 = SizeTokens.Size80.getDp(),
    )

    private val YGDefaultLayoutPadding: YGLayoutPadding = YGLayoutPadding(
        padding1 = SizeTokens.Size2.getDp(),
        padding2 = SizeTokens.Size4.getDp(),
        padding3 = SizeTokens.Size8.getDp(),
        padding4 = SizeTokens.Size10.getDp(),
        padding5 = SizeTokens.Size12.getDp(),
        padding6 = SizeTokens.Size16.getDp(),
        padding7 = SizeTokens.Size20.getDp(),
        padding8 = SizeTokens.Size24.getDp(),
        padding9 = SizeTokens.Size32.getDp(),
        padding10 = SizeTokens.Size40.getDp(),
    )

    val YGDefaultLayout: YGLayout = YGLayout(
        gap = YGDefaultLayoutGap,
        padding = YGDefaultLayoutPadding,
    )
}
