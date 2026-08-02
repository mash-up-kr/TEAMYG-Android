package com.teamyg.parfait.feature.camera.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.camera.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun PictureConfirmScreen(
    uri: String,
    source: PictureConfirmSource,
    onClickReCapture: () -> Unit,
    onClickConfirm: () -> Unit,
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = YGTheme.layout.padding.padding7,
                end = YGTheme.layout.padding.padding7,
                top = YGTheme.layout.padding.padding6,
                bottom = YGTheme.layout.padding.padding1,
            ),
    ) {
        YGCircleButton(
            iconResource = DesignSystemR.drawable.ic_close,
            type = YGCircleButtonType.Default,
            contentDescription = null,
            onClick = onClickClose,
            modifier = Modifier
                .align(Alignment.End),
        )
        Spacer(modifier = Modifier.height(YGTheme.layout.padding.padding4))
        val painter = rememberAsyncImagePainter(model = uri)

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val intrinsicSize = painter.intrinsicSize
            val isWidthBound = if (intrinsicSize.isSpecified && intrinsicSize.height > 0f) {
                val imageRatio = intrinsicSize.width / intrinsicSize.height
                val boxRatio = constraints.maxWidth.toFloat() / constraints.maxHeight.toFloat()
                imageRatio >= boxRatio
            } else {
                true
            }

            if (isWidthBound) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(YGAtomicColors.Gray.White)
                        .border(width = 1.dp, color = YGAtomicColors.Gray.Gray500),
                ) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap5))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
        ) {
            YGButton(
                text = stringResource(
                    when (source) {
                        PictureConfirmSource.CAMERA -> R.string.camera_picture_confirm_retake
                        PictureConfirmSource.GALLERY -> R.string.camera_picture_confirm_reselect
                    },
                ),
                buttonType = YGButtonType.Medium.Secondary,
                isEnabled = true,
                onClick = onClickReCapture,
                modifier = Modifier.weight(1f),
            )
            YGButton(
                text = stringResource(R.string.camera_picture_confirm_next),
                buttonType = YGButtonType.Medium.Primary,
                isEnabled = true,
                onClick = onClickConfirm,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@YGPreview
@Composable
private fun PreviewPictureConfirmScreen() = PreviewBox {
    PictureConfirmScreen(
        uri = "",
        source = PictureConfirmSource.CAMERA,
        onClickReCapture = {},
        onClickConfirm = {},
        onClickClose = {},
    )
}
