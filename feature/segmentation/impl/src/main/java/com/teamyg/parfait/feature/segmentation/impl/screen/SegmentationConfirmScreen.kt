package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarBackClose
import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.R

/**
 * 세그멘테이션으로 분리된 객체 이미지를 크게 확인하는 화면.
 *
 * TODO: 디자인 확정 후 문구와 레이아웃 조정 필요
 */
@Composable
internal fun SegmentationConfirmScreen(
    subjectImagePath: String,
    borderColorArgb: Int?,
    borderWidthDp: Float?,
    isNextEnabled: Boolean,
    isEditPhotoEnabled: Boolean,
    onClickBack: () -> Unit,
    onClickClose: () -> Unit,
    onClickEditPhoto: () -> Unit,
    onClickNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YGAtomicColors.Gray.White),
    ) {
        YGFloatingBarBackClose(
            onBackClick = onClickBack,
            onCloseClick = onClickClose,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = YGTheme.layout.padding.padding7),
        ) {
            val painter = rememberAsyncImagePainter(
                model = subjectImagePath,
                contentScale = ContentScale.Fit,
            )
            val painterState by painter.state.collectAsState()

            YGToppingCutoutImage(
                painter = painter,
                borderColor = borderColorArgb
                    ?.takeIf { painterState is AsyncImagePainter.State.Success }
                    ?.let { argb -> Color(argb) },
                borderWidth = (borderWidthDp ?: 0f).dp,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = YGTheme.layout.padding.padding7,
                    end = YGTheme.layout.padding.padding7,
                    top = YGTheme.layout.padding.padding5,
                    bottom = YGTheme.layout.padding.padding6,
                ),
        ) {
            YGButton(
                text = stringResource(R.string.segmentation_confirm_edit_photo),
                buttonType = YGButtonType.Medium.Secondary,
                isEnabled = isEditPhotoEnabled,
                onClick = onClickEditPhoto,
                modifier = Modifier.weight(1f),
            )
            YGButton(
                text = stringResource(R.string.segmentation_confirm_next),
                buttonType = YGButtonType.Medium.Primary,
                isEnabled = isNextEnabled,
                onClick = onClickNext,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@YGPreview
@Composable
private fun SegmentationConfirmScreenPreview() = PreviewBox {
    SegmentationConfirmScreen(
        subjectImagePath = "",
        borderColorArgb = null,
        borderWidthDp = null,
        isNextEnabled = true,
        isEditPhotoEnabled = true,
        onClickBack = {},
        onClickClose = {},
        onClickEditPhoto = {},
        onClickNext = {},
    )
}
