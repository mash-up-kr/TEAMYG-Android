package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.modal.YGModalQuitBackground
import com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarEdit
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.outline.rememberToppingOutlines
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.component.EditableToppingImage
import com.teamyg.parfait.feature.groups.canvas.impl.component.rememberEditableToppingDrawEntries
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingBorderStyle
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditUiState
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBackgroundPaletteColors
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/** 토핑은 배경을 고르는 참고로만 보인다 — 고를 수 없다는 것을 불투명도로 알린다 */
private const val BACKGROUND_TOPPING_ALPHA = 0.5f

@Composable
internal fun CanvasBGEditScreen(
    uiState: CanvasBGEditUiState,
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
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    top = YGTheme.layout.padding.padding4,
                    bottom = YGTheme.layout.padding.padding4,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 21.dp) // 21.dp 공통에 없음
                    .aspectRatio(CANVAS_AREA_ASPECT_RATIO)
                    .clipToBounds()
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

                // 배치가 모두 이 영역 대비 비율이라, 캔버스 메인과 같은 자리에 그리려면 실제 크기를 알아야 한다
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = maxWidth
                    val canvasHeight = maxHeight

                    val drawEntries = rememberEditableToppingDrawEntries(
                        toppings = uiState.toppings,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                    )

                    val outlines = rememberToppingOutlines(
                        models = drawEntries.map { it.topping.imageUrl },
                        retryKey = 0,
                    )

                    // 딤·입력 레이어·모서리 버튼·접근성 클릭을 붙이지 않는다
                    drawEntries.forEach { entry ->
                        EditableToppingImage(
                            entry = entry,
                            outline = outlines[entry.topping.imageUrl],
                            alpha = BACKGROUND_TOPPING_ALPHA,
                            onClick = null,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = YGTheme.layout.padding.padding6)
                .horizontalScroll(rememberScrollState())
                .padding(
                    horizontal = YGTheme.layout.padding.padding7,
                    vertical = YGTheme.layout.padding.padding2,
                ),
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

        YGFloatingBarEdit(
            title = stringResource(R.string.canvas_bg_edit_title),
            onCloseClick = onClickCloseButton,
            onConfirmClick = onClickConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = YGTheme.layout.padding.padding6,
                    bottom = YGTheme.layout.padding.padding1,
                ),
        )
    }

    if (uiState.showQuitDialog) {
        YGModalQuitBackground(
            onConfirmQuit = onQuitDialogConfirm,
            onDismiss = onQuitDialogCancel,
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
            .clickableYGNoRipple(onClick = onClick),
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
            ).clickableYGNoRipple(onClick = onClick),
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

/** 실제 리소스라야 painter 가 Success 에 닿아 테두리도 함께 그려진다 */
private const val PREVIEW_TOPPING_MODEL =
    "android.resource://com.teamyg.parfait.feature.groups.canvas.impl/drawable/nukkiii"

private val previewToppings = listOf(
    EditableTopping(
        parfaitImageId = 1L,
        isMine = true,
        imageUrl = PREVIEW_TOPPING_MODEL,
        positionX = 0.3f,
        positionY = 0.4f,
        border = ToppingBorderStyle(colorArgb = 0xFFFFFFFF.toInt(), widthDp = 4f),
    ),
    EditableTopping(
        parfaitImageId = 2L,
        isMine = false,
        imageUrl = PREVIEW_TOPPING_MODEL,
        positionX = 0.7f,
        positionY = 0.6f,
        scale = 1.2f,
        rotationDegrees = 15f,
    ),
)

@YGPreview
@Composable
private fun PreviewCanvasBGEditScreen() = PreviewBox {
    CanvasBGEditScreen(
        uiState = CanvasBGEditUiState(toppings = previewToppings),
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
