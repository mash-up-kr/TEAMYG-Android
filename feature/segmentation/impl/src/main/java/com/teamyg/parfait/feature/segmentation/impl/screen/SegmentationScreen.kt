package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teamyg.parfait.core.ui.preview.PreviewBox
import com.teamyg.parfait.core.ui.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationViewModel

@Composable
internal fun SegmentationScreen(
    viewModel: SegmentationViewModel,
    onClickBack: () -> Unit,
    onClickOk: (file: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        Button(
            onClick = { state.subjectImagePath?.let { onClickOk(it) } },
            enabled = state.subjectImagePath != null,
        ) {
            Text("완료")
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // 원본 이미지
            state.originBitmap?.let { originBitmap ->
                Image(
                    bitmap = originBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // 오버레이 이미지
            state.overlayBitmap?.let { overlayBitmap ->
                Image(
                    bitmap = overlayBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

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
        viewModel = viewModel(),
        onClickBack = {},
        modifier = Modifier.fillMaxSize(),
        onClickOk = {},
    )
}
