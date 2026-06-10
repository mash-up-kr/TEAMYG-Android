package com.teamyg.parfait.feature.gallery.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.feature.gallery.impl.component.GalleryImageGridComponent
import com.teamyg.parfait.feature.gallery.impl.component.GalleryPartialAccessBanner
import com.teamyg.parfait.feature.gallery.impl.component.GalleryPermissionRequestComponent
import com.teamyg.parfait.feature.gallery.impl.model.GalleryAccessLevel
import com.teamyg.parfait.feature.gallery.impl.model.GalleryAccessLevelPreviewParameterProvider
import com.teamyg.parfait.feature.gallery.impl.model.GalleryImageGroup
import com.teamyg.parfait.feature.gallery.impl.viewmodel.CustomGalleryPickerState
import com.tjyg.core.ui.preview.PreviewBox
import com.tjyg.core.ui.preview.YGPreview

@Composable
internal fun CustomGalleryPickerScreen(
    state: CustomGalleryPickerState,
    onClickGrantPermission: () -> Unit,
    onClickOpenSettings: () -> Unit,
    onClickManageMedia: () -> Unit,
    onClickImage: (String) -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.access.hasPermission) {
        true -> GalleryContent(
            isPartial = state.access.isPartial,
            isLoading = state.isLoading,
            isEmpty = state.isEmpty,
            groups = state.groups,
            onClickManageMedia = onClickManageMedia,
            onClickImage = onClickImage,
            onClickCancel = onClickCancel,
            modifier = modifier,
        )

        false -> GalleryPermissionRequestComponent(
            isInit = state.access == GalleryAccessLevel.INITIAL,
            isDeniedPermission = state.access.isDeniedPermission,
            onClickGrantPermission = onClickGrantPermission,
            onClickOpenSettings = onClickOpenSettings,
            modifier = modifier,
        )
    }
}

@Composable
private fun GalleryContent(
    isPartial: Boolean,
    isLoading: Boolean,
    isEmpty: Boolean,
    groups: List<GalleryImageGroup>,
    onClickManageMedia: () -> Unit,
    onClickImage: (String) -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(Color.Black)) {
        if (isPartial) {
            GalleryPartialAccessBanner(
                onClickManage = onClickManageMedia,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = Color.White)

                isEmpty -> Text(
                    text = "표시할 이미지가 없습니다.",
                    color = Color.White,
                )

                else -> GalleryImageGridComponent(
                    groups = groups,
                    onClickImage = onClickImage,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(16.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onClickCancel) {
                Text(text = "취소")
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewCustomGalleryPickerScreen(
    @PreviewParameter(GalleryAccessLevelPreviewParameterProvider::class) access: GalleryAccessLevel,
) = PreviewBox {
    CustomGalleryPickerScreen(
        state = CustomGalleryPickerState(
            access = access,
        ),
        onClickGrantPermission = {},
        onClickOpenSettings = {},
        onClickManageMedia = {},
        onClickImage = {},
        onClickCancel = {},
        modifier = Modifier.fillMaxSize(),
    )
}
