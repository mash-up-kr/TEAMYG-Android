package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarEditTab
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.CANVAS_ASPECT_RATIO
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_SIZE
import com.teamyg.parfait.feature.groups.canvas.impl.util.computeToppingButtonPoints
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingStrokeSize
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditUiState
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBackgroundPaletteColors
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasEditTab
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingItem
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
    onClickTopping: (CanvasToppingItem) -> Unit,
    onClickDeselectTopping: () -> Unit,
    onClickDeleteTopping: () -> Unit,
    onDeleteToppingDialogConfirm: () -> Unit,
    onDeleteToppingDialogCancel: () -> Unit,
    onClickEditTopping: () -> Unit,
    onToppingResizeDrag: (Offset) -> Unit,
    onToppingRotateDrag: (Offset) -> Unit,
    onToppingMoveDrag: (DpOffset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    top = if (uiState.selectedTab == CanvasEditTab.BACKGROUND) {
                        YGTheme.layout.padding.padding4
                    } else {
                        60.dp // 60.dp 공통에 없음
                    },
                    bottom = if (uiState.selectedTab == CanvasEditTab.BACKGROUND) {
                        YGTheme.layout.padding.padding4
                    } else {
                        14.dp // 14.dp 공통에 없음
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 21.dp) // 21.dp 공통에 없음
                    .aspectRatio(CANVAS_ASPECT_RATIO)
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

                if (uiState.selectedTab == CanvasEditTab.TOPPING) {
                    uiState.toppings.filterNot { it.isMine }.forEach { topping ->
                        CanvasToppingImage(topping = topping, onClick = onClickDeselectTopping)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(YGAtomicColors.Transparency.Black25)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClickDeselectTopping,
                            ),
                    )

                    uiState.toppings.filter { it.isMine }.forEach { topping ->
                        CanvasToppingImage(
                            topping = topping,
                            onClick = { onClickTopping(topping) },
                            // 선택된 토핑만 드래그로 이동할 수 있다
                            onDrag = if (topping.parfaitImageId == uiState.selectedToppingId) {
                                { dragAmount ->
                                    onToppingMoveDrag(
                                        with(density) { DpOffset(dragAmount.x.toDp(), dragAmount.y.toDp()) },
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }

                    uiState.toppings.find { it.parfaitImageId == uiState.selectedToppingId }?.let { selected ->
                        ToppingCornerButtons(
                            topping = selected,
                            onClickDelete = onClickDeleteTopping,
                            onClickEdit = onClickEditTopping,
                            onResizeDrag = onToppingResizeDrag,
                            onRotateDrag = onToppingRotateDrag,
                        )
                    }
                }
            }
        }

        if (uiState.selectedTab == CanvasEditTab.BACKGROUND) {
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
                .padding(
                    top = YGTheme.layout.padding.padding6,
                    bottom = YGTheme.layout.padding.padding1,
                ),
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

    if (uiState.showDeleteToppingDialog) {
        YGModalPopup(
            title = stringResource(R.string.canvas_bg_edit_topping_delete_dialog_title),
            body = stringResource(R.string.canvas_bg_edit_topping_delete_dialog_body),
            iconRes = DesignSystemR.drawable.ic_warning_round,
            secondaryText = stringResource(R.string.canvas_bg_edit_topping_delete_dialog_confirm),
            onSecondaryClick = onDeleteToppingDialogConfirm,
            primaryText = stringResource(R.string.canvas_bg_edit_topping_delete_dialog_cancel),
            onPrimaryClick = onDeleteToppingDialogCancel,
            onDismissRequest = onDeleteToppingDialogCancel,
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

/**
 * 캔버스 미리보기 박스 안, 토핑이 실제로 놓인 좌표([CanvasToppingItem.offsetX]/[offsetY])에 겹쳐 그리는 이미지.
 * [onClick]의 실제 동작(선택/선택 해제)도 호출하는 쪽에서 소유 여부에 따라
 * 다르게 넘겨준다. 선택 시 보이는 스트로크·버튼은 이 이미지와 함께 돌지 않아야 해서
 * [ToppingCornerButtons]에서 별도로 그린다. [onDrag]가 있으면(선택된 토핑) 드래그한 만큼 위치를 옮긴다.
 */
@Composable
private fun CanvasToppingImage(
    topping: CanvasToppingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDrag: ((Offset) -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .offset(x = topping.offsetX, y = topping.offsetY)
            .size(TOPPING_SIZE)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ).then(
                if (onDrag != null) Modifier.dragBy(topping.parfaitImageId, onDrag) else Modifier,
            ).graphicsLayer(
                scaleX = topping.scale,
                scaleY = topping.scale,
                rotationZ = topping.rotationDegrees,
            ),
    ) {
        Image(
            painter = painterResource(topping.imageResId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * 선택된 토핑의 스트로크와 그 네 모서리에 놓는 버튼들.
 * 좌측 상단=삭제, 우측 상단=크기조절(드래그 핸들), 좌측 하단=편집, 우측 하단=회전(드래그 핸들).
 */
@Composable
private fun ToppingCornerButtons(
    topping: CanvasToppingItem,
    onClickDelete: () -> Unit,
    onClickEdit: () -> Unit,
    onResizeDrag: (Offset) -> Unit,
    onRotateDrag: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val center = DpOffset(
        x = topping.offsetX + TOPPING_SIZE / 2,
        y = topping.offsetY + TOPPING_SIZE / 2,
    )
    val sizeAfterScale = TOPPING_SIZE * topping.scale
    val buttonPoints = computeToppingButtonPoints(
        center = center,
        sizeAfterScale = sizeAfterScale,
        rotationDegrees = topping.rotationDegrees,
    )

    Box(modifier = modifier) {
        ToppingSelectionStroke(
            center = center,
            sizeAfterScale = sizeAfterScale,
            rotationDegrees = topping.rotationDegrees,
        )
        YGCircleButton(
            iconResource = DesignSystemR.drawable.ic_close,
            type = YGCircleButtonType.Small,
            contentDescription = stringResource(R.string.canvas_bg_edit_topping_delete),
            onClick = onClickDelete,
            modifier = Modifier.centeredAt(buttonPoints.topLeft),
        )
        ToppingDragHandleButton(
            iconRes = DesignSystemR.drawable.ic_scale,
            contentDescription = stringResource(R.string.canvas_bg_edit_topping_resize),
            point = buttonPoints.topRight,
            toppingId = topping.parfaitImageId,
            onDrag = onResizeDrag,
        )
        YGCircleButton(
            iconResource = DesignSystemR.drawable.ic_edit,
            type = YGCircleButtonType.Small,
            contentDescription = stringResource(R.string.canvas_bg_edit_topping_edit),
            onClick = onClickEdit,
            modifier = Modifier.centeredAt(buttonPoints.bottomLeft),
        )
        ToppingDragHandleButton(
            iconRes = DesignSystemR.drawable.ic_rotate,
            contentDescription = stringResource(R.string.canvas_bg_edit_topping_rotate),
            point = buttonPoints.bottomRight,
            toppingId = topping.parfaitImageId,
            onDrag = onRotateDrag,
        )
    }
}

/**
 * 토핑과 함께 회전하는 흰색 2dp 점선 스트로크. [center]에 여백이 반영된 크기([toppingStrokeSize])로
 * 놓은 뒤 [rotationDegrees]만큼 [graphicsLayer]로 돌려, 토핑 자신의 회전을 그대로 따라가게 한다.
 */
@Composable
private fun ToppingSelectionStroke(
    center: DpOffset,
    sizeAfterScale: Dp,
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    val strokeSize = toppingStrokeSize(sizeAfterScale)

    Box(
        modifier = modifier
            .offset(x = center.x - strokeSize.width / 2, y = center.y - strokeSize.height / 2)
            .size(strokeSize)
            .graphicsLayer(rotationZ = rotationDegrees)
            .drawBehind {
                drawRect(
                    color = YGAtomicColors.Gray.White,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(7.5.dp.toPx(), 9.dp.toPx()),
                        ),
                    ),
                )
            },
    )
}

/**
 * 누르는 버튼이 아니라 잡고 끄는 핸들. 원형 아이콘 버튼 위에 드래그 제스처를 얹어서 쓴다.
 */
@Composable
private fun ToppingDragHandleButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    point: DpOffset,
    toppingId: Long,
    onDrag: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    YGCircleButton(
        iconResource = iconRes,
        type = YGCircleButtonType.Small,
        contentDescription = contentDescription,
        onClick = {},
        modifier = modifier
            .centeredAt(point)
            .dragBy(toppingId, onDrag),
    )
}

/** [point](캔버스 기준 절대 Dp 좌표)에 이 컴포저블 자신의 중심이 오도록 배치한다. */
private fun Modifier.centeredAt(point: DpOffset): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        val x = point.x.roundToPx() - placeable.width / 2
        val y = point.y.roundToPx() - placeable.height / 2
        placeable.placeRelative(x, y)
    }
}

/** [key]가 바뀌면 제스처를 새로 추적하며, 드래그한 만큼(픽셀) [onDrag]로 넘겨준다. */
private fun Modifier.dragBy(
    key: Any?,
    onDrag: (Offset) -> Unit,
): Modifier = pointerInput(key) {
    detectDragGestures { change, dragAmount ->
        change.consume()
        onDrag(dragAmount)
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
        onClickTopping = {},
        onClickDeselectTopping = {},
        onClickDeleteTopping = {},
        onDeleteToppingDialogConfirm = {},
        onDeleteToppingDialogCancel = {},
        onClickEditTopping = {},
        onToppingResizeDrag = {},
        onToppingRotateDrag = {},
        onToppingMoveDrag = {},
        modifier = Modifier.fillMaxSize(),
    )
}
