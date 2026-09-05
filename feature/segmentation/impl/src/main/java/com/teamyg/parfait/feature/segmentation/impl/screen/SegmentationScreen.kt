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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarBackClose
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.feature.segmentation.impl.component.GuideBanner
import com.teamyg.parfait.feature.segmentation.impl.component.SegmentationSubjectHighlight
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationState

/**
 * 대상을 하나 이상 얻은 뒤의 화면만 그린다 — 못 얻은 실패는 [SegmentationErrorScreen] 이
 * 받고, 둘 중 무엇을 띄울지는 상위 Route 가 [SegmentationState.isError] 로 고른다.
 */
@Composable
internal fun SegmentationScreen(
    state: SegmentationState,
    onClickBack: () -> Unit,
    onClickClose: () -> Unit,
    onClickCandidate: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(YGAtomicColors.Gray.White)) {
        YGFloatingBarBackClose(
            onBackClick = onClickBack,
            onCloseClick = onClickClose,
            modifier = Modifier.fillMaxWidth(),
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
                boundsList = state.candidates.map { it.bounds },
                onClickCandidate = onClickCandidate,
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
    boundsList: List<SegmentationBounds>,
    onClickCandidate: (index: Int) -> Unit,
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

            if (boundsList.isNotEmpty()) {
                SegmentationSubjectHighlight(
                    boundsList = boundsList,
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height,
                    onClickCandidate = onClickCandidate,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewSegmentationScreen() = PreviewBox {
    SegmentationScreen(
        state = SegmentationState(isLoading = false),
        onClickBack = {},
        onClickClose = {},
        onClickCandidate = {},
        modifier = Modifier.fillMaxSize(),
    )
}
