package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarEditTab
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.CANVAS_ASPECT_RATIO
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditUiState
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBackgroundPaletteColors
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasEditTab
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun CanvasBGEditScreen(
    uiState: CanvasBGEditUiState,
    onSelectTab: (CanvasEditTab) -> Unit,
    onSelectColor: (Color) -> Unit,
    onClickCamera: () -> Unit,
    onClickGallery: () -> Unit,
    onClickCloseButton: () -> Unit,
    onQuitDialogConfirm: () -> Unit,
    onQuitDialogCancel: () -> Unit,
    onClickConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = YGTheme.layout.padding.padding4,
                    start = 21.dp, // 21.dp 공통에 없음
                    end = 21.dp, // 21.dp 공통에 없음
                    bottom = YGTheme.layout.padding.padding4,
                ).aspectRatio(CANVAS_ASPECT_RATIO)
                .let { if (uiState.selectedImageUri == null) it.background(uiState.selectedColor) else it }
                .border(
                    width = 1.dp,
                    color = YGAtomicColors.Gray.Gray500,
                ),
        ) {
            uiState.selectedImageUri?.let { imageUri ->
                Image(
                    painter = rememberAsyncImagePainter(model = imageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = YGTheme.layout.padding.padding6)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = YGTheme.layout.padding.padding7, vertical = YGTheme.layout.padding.padding2),
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
        ) {
            PaletteActionCircle(
                iconResource = DesignSystemR.drawable.ic_gallery,
                contentDescription = null,
                onClick = onClickGallery,
                thumbnailUri = uiState.selectedImageUri.takeIf {
                    uiState.selectedImageSource == PictureConfirmSource.GALLERY
                },
            )
            PaletteActionCircle(
                iconResource = DesignSystemR.drawable.ic_camera,
                contentDescription = null,
                onClick = onClickCamera,
                thumbnailUri = uiState.selectedImageUri.takeIf {
                    uiState.selectedImageSource == PictureConfirmSource.CAMERA
                },
            )
            CanvasBackgroundPaletteColors.forEach { color ->
                PaletteColorCircle(
                    color = color,
                    isSelected = color == uiState.selectedColor && uiState.selectedImageUri == null,
                    onClick = { onSelectColor(color) },
                )
            }
        }

        YGFloatingBarEditTab(
            tabs = listOf(
                stringResource(R.string.canvas_bg_edit_tab_background),
                stringResource(R.string.canvas_bg_edit_tab_topping),
            ),
            selectedIndex = CanvasEditTab.entries.indexOf(uiState.selectedTab),
            onTabSelect = { index -> onSelectTab(CanvasEditTab.entries[index]) },
            onCloseClick = onClickCloseButton,
            onConfirmClick = onClickConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = YGTheme.layout.padding.padding1),
        )
    }

    if (uiState.showQuitDialog) {
        YGModalPopup(
            title = stringResource(R.string.canvas_bg_edit_quit_dialog_title),
            body = stringResource(R.string.canvas_bg_edit_quit_dialog_body),
            iconRes = DesignSystemR.drawable.ic_warning_round,
            secondaryText = stringResource(R.string.canvas_bg_edit_quit_dialog_confirm),
            onSecondaryClick = onQuitDialogConfirm,
            primaryText = stringResource(R.string.canvas_bg_edit_quit_dialog_cancel),
            onPrimaryClick = onQuitDialogCancel,
            onDismissRequest = onQuitDialogCancel,
        )
    }
}

@Composable
private fun PaletteActionCircle(
    iconResource: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailUri: String? = null,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(YGAtomicColors.Gray.Gray100)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnailUri != null) {
            Image(
                painter = rememberAsyncImagePainter(model = thumbnailUri),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    color = YGAtomicColors.Transparency.Black5,
                    shape = CircleShape,
                ),
        )
        Image(
            painter = painterResource(iconResource),
            contentDescription = if (thumbnailUri == null) contentDescription else null,
            colorFilter = ColorFilter.tint(
                if (thumbnailUri != null) YGAtomicColors.Gray.White else YGAtomicColors.Gray.Gray500,
            ),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun PaletteColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 1.dp,
                color = YGAtomicColors.Transparency.Black5,
                shape = CircleShape,
            ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(YGAtomicColors.Transparency.Black25),
            )
            Image(
                painter = painterResource(DesignSystemR.drawable.ic_check),
                contentDescription = null,
                colorFilter = ColorFilter.tint(YGAtomicColors.Gray.White),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@YGPreview
@Composable
private fun PreviewCanvasBGEditScreen() = PreviewBox {
    CanvasBGEditScreen(
        uiState = CanvasBGEditUiState(),
        onSelectTab = {},
        onSelectColor = {},
        onClickCamera = {},
        onClickGallery = {},
        onClickCloseButton = {},
        onQuitDialogConfirm = {},
        onQuitDialogCancel = {},
        onClickConfirm = {},
        modifier = Modifier.fillMaxSize(),
    )
}
