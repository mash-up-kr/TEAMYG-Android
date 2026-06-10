package com.teamyg.parfait.feature.gallery.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.ui.preview.PreviewBox
import com.teamyg.parfait.core.ui.preview.YGPreview

@Composable
internal fun GalleryPermissionRequestComponent(
    isInit: Boolean,
    isDeniedPermission: Boolean,
    onClickGrantPermission: () -> Unit,
    onClickOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.Black)
            .padding(PaddingValues(24.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        if (isInit.not()) {
            Text(
                text = when {
                    isDeniedPermission -> "갤러리 권한이 영구 거부되었습니다.\n설정에서 권한을 허용해 주세요."
                    else -> "갤러리 권한이 필요합니다."
                },
                color = Color.White,
            )

            when {
                isDeniedPermission -> Button(onClick = onClickOpenSettings) {
                    Text(text = "설정 열기")
                }

                else -> Button(onClick = onClickGrantPermission) {
                    Text(text = "권한 요청")
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewGalleryPermissionRequestComponent() = PreviewBox {
    GalleryPermissionRequestComponent(
        isInit = true,
        isDeniedPermission = false,
        onClickGrantPermission = {},
        onClickOpenSettings = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@YGPreview
@Composable
private fun PreviewGalleryPermissionRequestComponentPermanentlyDenied() = PreviewBox {
    GalleryPermissionRequestComponent(
        isInit = true,
        isDeniedPermission = true,
        onClickGrantPermission = {},
        onClickOpenSettings = {},
        modifier = Modifier.fillMaxSize(),
    )
}
