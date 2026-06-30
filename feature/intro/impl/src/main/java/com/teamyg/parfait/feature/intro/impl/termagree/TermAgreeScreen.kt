package com.teamyg.parfait.feature.intro.impl.termagree

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.ui.preview.PreviewBox
import com.teamyg.parfait.core.ui.preview.YGPreview

@Composable
internal fun TermAgreeScreen(
    state: TermAgreeState,
    modifier: Modifier = Modifier,
) {
}

@YGPreview
@Composable
private fun TermAgreeScreenPreview() = PreviewBox {
    TermAgreeScreen(
        modifier = Modifier.fillMaxSize(),
    )
}
