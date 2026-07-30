package com.teamyg.parfait.feature.camera.impl.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.camera.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun CameraPermissionRequestComponent(
    isInit: Boolean,
    permanentlyDenied: Boolean,
    onClickGrantPermission: () -> Unit,
    onClickOpenAppSettings: () -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(YGAtomicColors.Gray.White),
    ) {
        if (isInit) {
            YGIconButton(
                iconResource = DesignSystemR.drawable.ic_close,
                size = YGIconButtonSize.SIZE_44,
                contentDescription = null,
                onClick = onClickCancel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = YGTheme.layout.padding.padding6, end = YGTheme.layout.padding.padding7),
                isEnabled = true,
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(DesignSystemR.drawable.ic_warning_round),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    colorFilter = ColorFilter.tint(YGAtomicColors.Cherry.Cherry600),
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap3))
                Text(
                    text = stringResource(R.string.camera_permission_denied_title),
                    style = YGTheme.typography.title.t03SB,
                    color = YGAtomicColors.Gray.Gray900,
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap1))
                Text(
                    text = stringResource(R.string.camera_permission_denied_description),
                    style = YGTheme.typography.body.b02R,
                    color = YGAtomicColors.Gray.Gray500,
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap7))
                YGButton(
                    text = stringResource(R.string.camera_permission_open_settings),
                    buttonType = YGButtonType.Medium.Primary,
                    isEnabled = true,
                    onClick = onClickOpenAppSettings,
                )
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewCameraPermissionRequestComponent() = PreviewBox {
    CameraPermissionRequestComponent(
        isInit = true,
        permanentlyDenied = false,
        onClickGrantPermission = {},
        onClickOpenAppSettings = {},
        onClickCancel = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@YGPreview
@Composable
private fun PreviewCameraPermissionRequestComponentPermanentlyDenied() = PreviewBox {
    CameraPermissionRequestComponent(
        isInit = true,
        permanentlyDenied = true,
        onClickGrantPermission = {},
        onClickOpenAppSettings = {},
        onClickCancel = {},
        modifier = Modifier.fillMaxSize(),
    )
}
