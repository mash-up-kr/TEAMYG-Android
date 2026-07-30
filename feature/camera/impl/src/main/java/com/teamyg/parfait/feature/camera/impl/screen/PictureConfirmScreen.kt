package com.teamyg.parfait.feature.camera.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.feature.camera.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun PictureConfirmScreen(
    uri: String,
    onClickReCapture: () -> Unit,
    onClickConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = YGTheme.layout.padding.padding7),
    ) {
        Spacer(modifier = Modifier.height(YGTheme.layout.padding.padding6))
        YGIconButton(
            iconResource = DesignSystemR.drawable.ic_close,
            size = YGIconButtonSize.SIZE_44,
            contentDescription = null,
            onClick = {},
            modifier = Modifier
                .align(Alignment.End),
            isEnabled = true,
        )
        Spacer(modifier = Modifier.height(YGTheme.layout.padding.padding4))
        val painter = rememberAsyncImagePainter(model = uri)

        Box(modifier = Modifier.weight(1f).fillMaxWidth().border(width = 1.dp, color = YGAtomicColors.Gray.Gray500)) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(YGTheme.layout.padding.padding6))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
        ) {
            YGButton(
                text = stringResource(R.string.camera_picture_confirm_retake),
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
        Spacer(modifier = Modifier.height(YGTheme.layout.padding.padding9))
    }
}

@YGPreview
@Composable
private fun PreviewPictureConfirmScreen() = PreviewBox {
    PictureConfirmScreen(
        uri = "",
        onClickReCapture = {},
        onClickConfirm = {},
    )
}
