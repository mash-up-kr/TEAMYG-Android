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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.component.ygedittabbutton.YGEditTabButton
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.util.PictureConfirmSourceSaver
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

private const val CANVAS_EDIT_BOX_ASPECT_RATIO = 9f / 16f

private enum class CanvasEditTab { BACKGROUND, TOPPING }

private val ColorSaver = Saver<Color, Int>(
    save = { it.toArgb() },
    restore = { Color(it) },
)

private val CanvasBackgroundPaletteColors = listOf(
    YGAtomicColors.Gray.White,
    YGAtomicColors.Gray.Black,
    YGAtomicColors.Cherry.Cherry200,
    Color(0xFFFCE7C2),
    Color(0xFFF9F9AB),
    Color(0xFFC5FFD7),
    Color(0xFFC2E4FC),
    Color(0xFFDCC2FC),
)

@Composable
internal fun CanvasBGEditScreen(
    backgroundImageUri: String?,
    backgroundImageSource: PictureConfirmSource?,
    onClickClose: () -> Unit,
    onClickConfirm: (YGCanvasBackground) -> Unit,
    onClickCamera: () -> Unit,
    onClickGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(CanvasEditTab.BACKGROUND) }
    // 저장된 배경 없을때만 하얀색 아니면 기존 배경 불러와야함
    var selectedColor by rememberSaveable(stateSaver = ColorSaver) {
        mutableStateOf(CanvasBackgroundPaletteColors.first())
    }
    var selectedImageUri by rememberSaveable { mutableStateOf(backgroundImageUri) }
    var selectedImageSource by rememberSaveable(stateSaver = PictureConfirmSourceSaver) {
        mutableStateOf(backgroundImageSource)
    }
    var showQuitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(backgroundImageUri, backgroundImageSource) {
        if (backgroundImageUri != null) {
            selectedImageUri = backgroundImageUri
            selectedImageSource = backgroundImageSource
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = YGTheme.layout.padding.padding4,
                    start = 21.dp, // 21.dp 공통에 없음
                    end = 21.dp, // 21.dp 공통에 없음
                    bottom = YGTheme.layout.padding.padding4,
                ).aspectRatio(CANVAS_EDIT_BOX_ASPECT_RATIO)
                .let { if (selectedImageUri == null) it.background(selectedColor) else it }
                .border(
                    width = 1.dp,
                    color = YGAtomicColors.Gray.Gray500,
                ),
        ) {
            selectedImageUri?.let { imageUri ->
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
                thumbnailUri = selectedImageUri.takeIf { selectedImageSource == PictureConfirmSource.GALLERY },
            )
            PaletteActionCircle(
                iconResource = DesignSystemR.drawable.ic_camera,
                contentDescription = null,
                onClick = onClickCamera,
                thumbnailUri = selectedImageUri.takeIf { selectedImageSource == PictureConfirmSource.CAMERA },
            )
            CanvasBackgroundPaletteColors.forEach { color ->
                PaletteColorCircle(
                    color = color,
                    isSelected = color == selectedColor && selectedImageUri == null,
                    onClick = {
                        selectedColor = color
                        selectedImageUri = null
                        selectedImageSource = null
                    },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = YGTheme.layout.padding.padding6,
                    start = YGTheme.layout.padding.padding7,
                    end = YGTheme.layout.padding.padding7,
                    bottom = YGTheme.layout.padding.padding1,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            YGCircleButton(
                iconResource = DesignSystemR.drawable.ic_close,
                type = YGCircleButtonType.Default,
                contentDescription = null,
                onClick = { showQuitDialog = true },
            )

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
            ) {
                YGEditTabButton(
                    text = stringResource(R.string.canvas_bg_edit_tab_background),
                    isSelected = selectedTab == CanvasEditTab.BACKGROUND,
                    onClick = { selectedTab = CanvasEditTab.BACKGROUND },
                )
                YGEditTabButton(
                    text = stringResource(R.string.canvas_bg_edit_tab_topping),
                    isSelected = selectedTab == CanvasEditTab.TOPPING,
                    onClick = { selectedTab = CanvasEditTab.TOPPING },
                )
            }
            YGCircleButton(
                iconResource = DesignSystemR.drawable.ic_check,
                type = YGCircleButtonType.Default,
                contentDescription = null,
                onClick = {
                    val imageUri = selectedImageUri
                    onClickConfirm(
                        if (imageUri != null) {
                            YGCanvasBackground.Image(url = imageUri)
                        } else {
                            YGCanvasBackground.Solid(selectedColor)
                        },
                    )
                },
            )
        }
    }

    if (showQuitDialog) {
        YGModalPopup(
            title = stringResource(R.string.canvas_bg_edit_quit_dialog_title),
            body = stringResource(R.string.canvas_bg_edit_quit_dialog_body),
            iconRes = DesignSystemR.drawable.ic_warning_round,
            secondaryText = stringResource(R.string.canvas_bg_edit_quit_dialog_confirm),
            onSecondaryClick = onClickClose,
            primaryText = stringResource(R.string.canvas_bg_edit_quit_dialog_cancel),
            onPrimaryClick = { showQuitDialog = false },
            onDismissRequest = { showQuitDialog = false },
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
            )
        }
    }
}

@YGPreview
@Composable
private fun PreviewCanvasBGEditScreen() = PreviewBox {
    CanvasBGEditScreen(
        backgroundImageUri = null,
        backgroundImageSource = null,
        onClickClose = {},
        onClickConfirm = {},
        onClickCamera = {},
        onClickGallery = {},
        modifier = Modifier.fillMaxSize(),
    )
}
