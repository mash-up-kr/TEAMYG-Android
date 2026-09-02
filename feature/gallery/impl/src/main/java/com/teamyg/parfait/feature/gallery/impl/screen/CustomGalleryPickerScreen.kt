package com.teamyg.parfait.feature.gallery.impl.screen

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarClose
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarTitle
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.feature.gallery.impl.R
import com.teamyg.parfait.feature.gallery.impl.component.GalleryImageGridComponent
import com.teamyg.parfait.feature.gallery.impl.component.GalleryPermissionRequestComponent
import com.teamyg.parfait.feature.gallery.impl.model.GalleryAccessLevelPreviewParameterProvider
import com.teamyg.parfait.feature.gallery.impl.viewmodel.CustomGalleryPickerState
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.permission.GalleryPermissionManager
import com.teamyg.parfait.domain.model.GalleryImageGroup
import com.teamyg.parfait.domain.model.image.RecentImage
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun CustomGalleryPickerScreen(
    state: CustomGalleryPickerState,
    toastPolicy: YGToastPolicy,
    onClickGrantPermission: () -> Unit,
    onClickOpenSettings: () -> Unit,
    onClickManageMedia: () -> Unit,
    onClickImage: (String) -> Unit,
    onClickCutoutImage: (RecentImage) -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.access.hasPermission) {
        true -> GalleryContent(
            isPartial = state.access.isPartial,
            isLoading = state.isLoading,
            isEmpty = state.isEmpty,
            groups = state.groups,
            recentImages = state.recentImages,
            toastPolicy = toastPolicy,
            onClickManageMedia = onClickManageMedia,
            onClickImage = onClickImage,
            onClickCutoutImage = onClickCutoutImage,
            onClickCancel = onClickCancel,
            modifier = modifier,
        )

        false -> GalleryPermissionRequestComponent(
            isDeniedPermission = state.access.isDeniedPermission,
            onClickGrantPermission = onClickGrantPermission,
            onClickOpenSettings = onClickOpenSettings,
            onClickCancel = onClickCancel,
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
    recentImages: List<RecentImage>,
    toastPolicy: YGToastPolicy,
    onClickManageMedia: () -> Unit,
    onClickImage: (String) -> Unit,
    onClickCutoutImage: (RecentImage) -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(YGAtomicColors.Gray.White)) {
        if (isEmpty) {
            YGFloatingBarClose(
                onCloseClick = onClickCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            YGFloatingBarTitle(
                title = stringResource(R.string.gallery_today_photos_title),
                onCloseClick = onClickCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap5))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isLoading -> CircularProgressIndicator(color = Color.White)

                    isEmpty -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
                    ) {
                        Image(
                            painter = painterResource(DesignSystemR.drawable.image_gallery_empty),
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(R.string.gallery_empty_today_description),
                            color = YGAtomicColors.Gray.Gray300,
                            style = YGTheme.typography.body.b02R,
                            textAlign = TextAlign.Center,
                        )
                    }

                    else -> GalleryImageGridComponent(
                        groups = groups,
                        recentImages = recentImages,
                        onClickImage = onClickImage,
                        onClickCutoutImage = onClickCutoutImage,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            YGToastHost(
                policy = toastPolicy,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            )
        }

        if (isPartial) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = YGTheme.layout.padding.padding1),
                contentAlignment = Alignment.Center,
            ) {
                YGButton(
                    text = stringResource(R.string.gallery_reselect_photos),
                    buttonType = YGButtonType.Medium.Primary,
                    isEnabled = true,
                    onClick = onClickManageMedia,
                )
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewCustomGalleryPickerScreen(
    @PreviewParameter(GalleryAccessLevelPreviewParameterProvider::class)
    access: GalleryPermissionManager.GalleryAccessLevel,
) = PreviewBox {
    CustomGalleryPickerScreen(
        state = CustomGalleryPickerState(
            access = access,
        ),
        toastPolicy = rememberYGToastPolicy(),
        onClickGrantPermission = {},
        onClickOpenSettings = {},
        onClickManageMedia = {},
        onClickImage = {},
        onClickCutoutImage = {},
        onClickCancel = {},
        modifier = Modifier.fillMaxSize(),
    )
}
