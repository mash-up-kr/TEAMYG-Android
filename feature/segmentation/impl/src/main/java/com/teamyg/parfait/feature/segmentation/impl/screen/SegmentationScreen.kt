package com.teamyg.parfait.feature.segmentation.impl.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.feature.segmentation.impl.component.GuideBanner
import com.teamyg.parfait.feature.segmentation.impl.component.SegmentationSubjectHighlight
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationState
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun SegmentationScreen(
    state: SegmentationState,
    onClickBack: () -> Unit,
    onClickSubject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> SegmentationLoadingScreen(
            onClickClose = onClickBack,
            modifier = modifier,
        )

        state.isError -> SegmentationErrorScreen(
            onClickClose = onClickBack,
            modifier = modifier,
        )

        else -> SegmentationContent(
            state = state,
            onClickBack = onClickBack,
            onClickSubject = onClickSubject,
            modifier = modifier,
        )
    }
}

@Composable
private fun SegmentationContent(
    state: SegmentationState,
    onClickBack: () -> Unit,
    onClickSubject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(YGAtomicColors.Gray.White)) {
        YGCircleButton(
            iconResource = DesignSystemR.drawable.ic_caret_left,
            type = YGCircleButtonType.Default,
            contentDescription = "뒤로가기",
            onClick = onClickBack,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(
                    start = YGTheme.layout.padding.padding7,
                    end = YGTheme.layout.padding.padding7,
                    top = YGTheme.layout.padding.padding6,
                ),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    start = YGTheme.layout.padding.padding7,
                    top = YGTheme.layout.padding.padding6,
                    end = YGTheme.layout.padding.padding7,
                    bottom = YGTheme.layout.padding.padding6,
                ),
        ) {
            GuideBanner(modifier = Modifier.fillMaxWidth())

            SegmentationResultImage(
                originBitmap = state.originBitmap,
                subjectBounds = state.subjectBounds,
                onClickSubject = onClickSubject,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

/**
 * 촬영한 원본 이미지 위에 세그멘테이션 결과(딤 + dashed Rectangle)를 겹쳐 보여준다.
 */
@Composable
private fun SegmentationResultImage(
    originBitmap: Bitmap?,
    subjectBounds: SegmentationBounds?,
    onClickSubject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        originBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            if (subjectBounds != null) {
                SegmentationSubjectHighlight(
                    bounds = subjectBounds,
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height,
                    onClickSubject = onClickSubject,
                    modifier = Modifier.matchParentSize(),
                )
            }
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
        onClickSubject = {},
        modifier = Modifier.fillMaxSize(),
    )
}
