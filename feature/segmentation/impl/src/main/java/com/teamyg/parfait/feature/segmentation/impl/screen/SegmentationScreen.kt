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
 * 로딩과 실패는 이 화면이 그리지 않는다 — 로딩은 `YGScaffoldV2` 의 공통 오버레이가,
 * 실패는 공통 에러 토스트가 맡는다(둘 다 Route 가 건다). 그래서 여기 남는 것은
 * "객체 인식이 끝난 뒤의 화면" 하나뿐이다.
 *
 * 실패해도 이 화면이 그대로 보인다. 인식된 대상이 없으니 하이라이트만 빠진 채
 * 원본 사진이 남고, 사용자는 토스트를 읽고 뒤로 가 다른 사진을 고른다.
 */
@Composable
internal fun SegmentationScreen(
    state: SegmentationState,
    onClickBack: () -> Unit,
    onClickClose: () -> Unit,
    onClickSubject: () -> Unit,
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

@YGPreview
@Composable
private fun PreviewSegmentationScreen() = PreviewBox {
    SegmentationScreen(
        state = SegmentationState(isLoading = false),
        onClickBack = {},
        onClickClose = {},
        onClickSubject = {},
        modifier = Modifier.fillMaxSize(),
    )
}
