package com.teamyg.parfait.feature.intro.impl.termagree

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.ui.preview.PreviewBox
import com.teamyg.parfait.core.ui.preview.YGPreview

@Composable
internal fun TermAgreeScreen(
    state: TermAgreeState,
    onClickTermAgree: (index: Int, newSelected: Boolean) -> Unit,
    onClickTermLandingUrl: (landingUrl: String?) -> Unit,
    onClickAgreeAllTerm: (newSelected: Boolean) -> Unit,
    onClickNextButton: () -> Unit,
    onClickBackButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
}

@YGPreview
@Composable
private fun TermAgreeScreenPreview() = PreviewBox {
    TermAgreeScreen(
        state = TermAgreeState(),
        onClickTermAgree = { _, _ -> },
        onClickTermLandingUrl = {},
        onClickAgreeAllTerm = {},
        onClickNextButton = {},
        onClickBackButton = {},
        modifier = Modifier.fillMaxSize(),
    )
}
