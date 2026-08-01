package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.component.SegmentationSubjectHighlight
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationState

@Composable
internal fun SegmentationScreen(
    state: SegmentationState,
    onClickBack: () -> Unit,
    onClickOk: (file: String) -> Unit,
    onClickSubject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> SegmentationLoadingScreen(modifier = modifier)

        state.isError -> SegmentationErrorScreen(
            onClickBack = onClickBack,
            modifier = modifier,
        )

        else -> SegmentationContent(
            state = state,
            onClickBack = onClickBack,
            onClickOk = onClickOk,
            onClickSubject = onClickSubject,
            modifier = modifier,
        )
    }
}

@Composable
private fun SegmentationContent(
    state: SegmentationState,
    onClickBack: () -> Unit,
    onClickOk: (file: String) -> Unit,
    onClickSubject: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // 감지된 객체를 dashed Rectangle 로 표시하고 바깥을 어둡게 덮는다
            val originBitmap = state.originBitmap
            val subjectBounds = state.subjectBounds
            if (originBitmap != null && subjectBounds != null) {
                SegmentationSubjectHighlight(
                    bounds = subjectBounds,
                    imageWidth = originBitmap.width,
                    imageHeight = originBitmap.height,
                    onClickSubject = onClickSubject,
                    modifier = Modifier.matchParentSize(),
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

private class SegmentationScreenPreviewParameterProvider :
    PreviewParameterProvider<SegmentationState> {
    override val values: Sequence<SegmentationState>
        get() = sequenceOf(
            SegmentationState(isLoading = true),
            SegmentationState(isLoading = false, isError = true),
            SegmentationState(isLoading = false),
        )
}

@YGPreview
@Composable
private fun PreviewSegmentationScreen(
    @PreviewParameter(SegmentationScreenPreviewParameterProvider::class) state: SegmentationState,
) = PreviewBox {
    SegmentationScreen(
        state = state,
        onClickBack = {},
        onClickOk = {},
        onClickSubject = {},
        modifier = Modifier.fillMaxSize(),
    )
}
