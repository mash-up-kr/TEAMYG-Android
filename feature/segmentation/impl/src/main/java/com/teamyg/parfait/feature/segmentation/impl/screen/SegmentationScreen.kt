package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.ui.preview.PreviewBox
import com.teamyg.parfait.core.ui.preview.YGPreview

@Composable
internal fun SegmentationScreen(
    sourceImageUri: String,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        Text(text = "누끼 화면 // sourceImageUri: $sourceImageUri")

        Button(
            onClick = onClickBack,
        ) {
            Text(text = "뒤로")
        }
    }
}

@YGPreview
@Composable
private fun PreviewSegmentationScreen() = PreviewBox {
    SegmentationScreen(
        sourceImageUri = "sourceImageUri",
        onClickBack = {},
        modifier = Modifier.fillMaxSize(),
    )
}
