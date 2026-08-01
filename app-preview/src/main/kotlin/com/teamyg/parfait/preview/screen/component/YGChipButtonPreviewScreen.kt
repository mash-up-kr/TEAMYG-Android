package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGChipButtonPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        YGTopBarBack(onIconClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                PreviewSection("CherrySubtle") {
                    YGChipButton(
                        text = "칩",
                        colors = YGChipButtonColorsDefaults.CherrySubtle,
                        onClick = {},
                    )
                }
            }
            item {
                PreviewSection("CherrySolid") {
                    YGChipButton(
                        text = "칩",
                        colors = YGChipButtonColorsDefaults.CherrySolid,
                        onClick = {},
                    )
                }
            }
            item {
                PreviewSection("with start icon") {
                    YGChipButton(
                        text = "추가",
                        colors = YGChipButtonColorsDefaults.CherrySolid,
                        onClick = {},
                        startIconResource = R.drawable.ic_plus,
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGChipButtonPreviewScreen() = PreviewBox {
    YGChipButtonPreviewScreen(
        onBack = {},
    )
}
