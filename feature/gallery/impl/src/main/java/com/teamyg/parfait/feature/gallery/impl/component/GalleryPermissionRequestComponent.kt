package com.teamyg.parfait.feature.gallery.impl.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarClose
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.gallery.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun GalleryPermissionRequestComponent(
    isDeniedPermission: Boolean,
    onClickGrantPermission: () -> Unit,
    onClickOpenSettings: () -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 닫기 줄을 세로로 쌓지 않고 겹쳐 놓는다 — 줄이 자리를 차지하면 안내 블록이 그 아래
    // 남은 공간의 가운데로 앉아, 화면 기준으로는 줄 높이의 절반만큼 내려가 보인다
    Box(
        modifier = modifier
            .background(YGAtomicColors.Gray.White),
    ) {
        if (isDeniedPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = YGTheme.layout.padding.padding3),
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
                    text = stringResource(R.string.gallery_permission_denied_title),
                    style = YGTheme.typography.title.t03SB,
                    color = YGAtomicColors.Gray.Gray900,
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap1))
                Text(
                    text = stringResource(R.string.gallery_permission_denied_description),
                    style = YGTheme.typography.body.b02R,
                    color = YGAtomicColors.Gray.Gray500,
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap7))
                YGButton(
                    text = stringResource(R.string.gallery_permission_open_settings),
                    buttonType = YGButtonType.Medium.Primary,
                    isEnabled = true,
                    onClick = onClickOpenSettings,
                )
            }

            // 인셋은 이 화면을 띄우는 Scaffold 가 이미 물려 준다 — 여기서 한 번 더 물면
            // 상태바 높이만큼 아래로 밀려 닫기 버튼이 설계보다 깊이 내려간다
            YGFloatingBarClose(
                onCloseClick = onClickCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@YGPreview
@Composable
private fun PreviewGalleryPermissionRequestComponent() = PreviewBox {
    GalleryPermissionRequestComponent(
        isDeniedPermission = false,
        onClickGrantPermission = {},
        onClickOpenSettings = {},
        onClickCancel = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@YGPreview
@Composable
private fun PreviewGalleryPermissionRequestComponentPermanentlyDenied() = PreviewBox {
    GalleryPermissionRequestComponent(
        isDeniedPermission = true,
        onClickGrantPermission = {},
        onClickOpenSettings = {},
        onClickCancel = {},
        modifier = Modifier.fillMaxSize(),
    )
}
