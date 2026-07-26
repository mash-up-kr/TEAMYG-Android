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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

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
        if (isInit && !permanentlyDenied) {
            YGIconButton(
                iconResource = R.drawable.ic_close,
                size = YGIconButtonSize.SIZE_44,
                contentDescription = null,
                onClick = onClickCancel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(YGTheme.layout.padding.padding3),
                isEnabled = true,
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_warning_round),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(YGAtomicColors.Cherry.Cherry600),
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap3))
                Text(
                    text = "카메라 권한이 없어요",
                    style = YGTheme.typography.title.t03SB,
                    color = YGAtomicColors.Gray.Gray900,
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap1))
                Text(
                    text = "설정에서 카메라 권한을 허용해 주세요",
                    style = YGTheme.typography.body.b02R,
                    color = YGAtomicColors.Gray.Gray500,
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap7))
                YGButton(
                    text = "설정으로 이동",
                    buttonType = YGButtonType.SmallSquare,
                    isEnabled = true,
                    onClick = onClickOpenAppSettings,
                )
            }
        }

//        if (isInit) {
//            when (permanentlyDenied) {
//                true -> {
//                    Text(
//                        text = "카메라 권한이 거부되어 있습니다.\n설정에서 권한을 허용해주세요.",
//                        color = Color.White,
//                    )
//                    Button(onClick = onClickOpenAppSettings) {
//                        Text(text = "설정 열기")
//                    }
//                }
//
//                false -> {
//                    Text(
//                        text = "카메라 권한이 필요합니다.",
//                        color = Color.White,
//                    )
//                    Button(onClick = onClickGrantPermission) {
//                        Text(text = "권한 요청")
//                    }
//                }
//            }
//        }
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
