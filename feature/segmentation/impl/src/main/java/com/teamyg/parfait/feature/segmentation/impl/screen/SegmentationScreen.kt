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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygalert.YGAlert
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarBackTitleClose
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.feature.segmentation.impl.R
import com.teamyg.parfait.feature.segmentation.impl.component.SegmentationSubjectHighlight
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationState

/** Figma 에서 상단 바와 사진은 38dp, 얼럿은 13dp 떨어진다 */
private val IMAGE_TOP_GAP = 38.dp
private val ALERT_TOP_GAP = 13.dp

/** 후보를 하나 이상 얻은 뒤의 상태만 그린다 */
@Composable
internal fun SegmentationScreen(
    state: SegmentationState,
    onClickBack: () -> Unit,
    onClickClose: () -> Unit,
    onClickCandidate: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    alert: @Composable () -> Unit = {},
) {
    Column(modifier = modifier.background(YGAtomicColors.Gray.White)) {
        YGFloatingBarBackTitleClose(
            title = stringResource(R.string.segmentation_title),
            onBackClick = onClickBack,
            onCloseClick = onClickClose,
            modifier = Modifier.fillMaxWidth(),
        )

        // 얼럿이 화면 폭을 써야 해서 좌우 여백은 사진에만 준다
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            SegmentationResultImage(
                originBitmap = state.originBitmap,
                boundsList = state.candidates.map { it.bounds },
                onClickCandidate = onClickCandidate,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = YGTheme.layout.padding.padding7,
                        top = IMAGE_TOP_GAP,
                        end = YGTheme.layout.padding.padding7,
                        bottom = YGTheme.layout.padding.padding6,
                    ),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ALERT_TOP_GAP),
            ) {
                alert()
            }
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
        state = SegmentationState(isAnalyzing = false),
        onClickBack = {},
        onClickClose = {},
        onClickCandidate = {},
        alert = {
            YGAlert(
                title = stringResource(R.string.segmentation_manual_select_alert_title),
                sub = stringResource(R.string.segmentation_manual_select_alert_sub),
                buttonText = stringResource(R.string.segmentation_manual_select_alert_button),
                onButtonClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}
