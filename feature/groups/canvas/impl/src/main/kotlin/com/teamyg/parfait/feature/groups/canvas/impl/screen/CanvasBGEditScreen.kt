package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import com.teamyg.parfait.core.util.android.extension.centeredAt
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingSelectionStroke
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingResizeHandleButton
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingRotateHandleButton
import com.teamyg.parfait.feature.groups.canvas.impl.component.toppingDragInput
import com.teamyg.parfait.feature.groups.canvas.impl.component.toppingTapInput
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarEditTab
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.util.computeToppingButtonPoints
import com.teamyg.parfait.feature.groups.canvas.impl.util.rememberToppingAlphaMasks
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingCenter
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingImageSize
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingLongSide
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
    onToppingResize: (Float) -> Unit,
    onToppingRotate: (Float) -> Unit,
    onToppingMoveDrag: (deltaX: Float, deltaY: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
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

                if (uiState.selectedTab == CanvasEditTab.TOPPING) {
                    // 배치가 모두 이 영역 대비 비율이라, 캔버스 메인과 같은 자리에 그리려면
                    // 실제 크기를 알아야 한다
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = maxWidth
                        val canvasHeight = maxHeight
                        val density = LocalDensity.current
                        val canvasWidthPx = with(density) { canvasWidth.toPx() }
                        val canvasHeightPx = with(density) { canvasHeight.toPx() }

                        val entries = rememberBGEditHitEntries(
                            toppings = uiState.toppings,
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                        )
                        val myEntries = entries.filter { it.topping.isMine }
                        val selectedEntry = myEntries.firstOrNull {
                            it.topping.parfaitImageId == uiState.selectedToppingId
                        }

                        // 남의 토핑
                        entries.filterNot { it.topping.isMine }.forEach { entry ->
                            CanvasToppingImage(
                                entry = entry,
                                canvasWidth = canvasWidth,
                                canvasHeight = canvasHeight,
                                onClick = onClickDeselectTopping,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(YGAtomicColors.Transparency.Black25),
                        )

                        // 내 토핑
                        myEntries.forEach { entry ->
                            CanvasToppingImage(
                                entry = entry,
                                canvasWidth = canvasWidth,
                                canvasHeight = canvasHeight,
                                onClick = { onClickTopping(entry.topping) },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .toppingTapInput(
                                    entries = { myEntries.map { it.topping to it.target } },
                                    keyOf = { it.parfaitImageId },
                                    onHit = onClickTopping,
                                    onMiss = onClickDeselectTopping,
                                ).toppingDragInput(
                                    targetAt = { selectedEntry?.target },
                                    onDrag = { amount ->
                                        onToppingMoveDrag(
                                            amount.x / canvasWidthPx,
                                            amount.y / canvasHeightPx,
                                        )
                                    },
                                ),
                        )

                        selectedEntry?.let { entry ->
                            ToppingCornerButtons(
                                entry = entry,
                                canvasWidth = canvasWidth,
                                canvasHeight = canvasHeight,
                                onClickDelete = onClickDeleteTopping,
                                onClickEdit = onClickEditTopping,
                                onResize = onToppingResize,
                                onRotate = onToppingRotate,
                            )
                        }
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

private data class BGEditHitEntry(
    val topping: CanvasToppingItem,
    // Painter 로 좁히면 state 를 잃어 테두리 조건을 볼 수 없다
    val painter: AsyncImagePainter,
    val target: ToppingHitTarget,
)

/** 배경 편집이 그리는 대상. 편집본이 있으면 그쪽이고, 그 파일은 투명 여백이 잘려 있다. */
private val CanvasToppingItem.drawnModel: String
    get() = editedImagePath ?: imageUrl

/**
 * 그리기와 판정이 같은 painter 를 본다. 각각 만들면 비율이 서로 다른 시점의 값이 될 수 있다.
 *
 * 마스크는 판정에 실제로 쓰는 내 토핑만 요청한다. 남의 토핑은 탭 대상도 드래그 대상도 아니고
 * 그리는 데는 painter 만 있으면 되므로, 마스크가 없어도 화면이 달라지지 않는다.
 */
@Composable
private fun rememberBGEditHitEntries(
    toppings: List<CanvasToppingItem>,
    canvasWidth: Dp,
    canvasHeight: Dp,
): List<BGEditHitEntry> {
    val masks = rememberToppingAlphaMasks(
        toppings.filter { it.isMine }.map { it.drawnModel },
    )
    val density = LocalDensity.current

    return toppings.map { topping ->
        key(topping.parfaitImageId) {
            val painter = rememberAsyncImagePainter(model = topping.drawnModel)
            val painterState by painter.state.collectAsState()
            val intrinsicSize = painter.intrinsicSize

            val aspectRatio = if (intrinsicSize.isSpecified && intrinsicSize.height > 0f) {
                intrinsicSize.width / intrinsicSize.height
            } else {
                0f
            }

            val imageSize = toppingImageSize(
                longSide = toppingLongSide(canvasWidth, topping.scale),
                aspectRatio = aspectRatio,
            )
            val center = toppingCenter(
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                positionX = topping.positionX,
                positionY = topping.positionY,
            )

            // 테두리를 그리지 않는 상태에서는 판정도 넓히지 않는다 — 그리지 않은 링만큼 부풀면
            // 판정이 외형과 어긋난다
            val drawnBorderWidth = topping.borderLayers
                .firstOrNull()
                ?.takeIf { painterState is AsyncImagePainter.State.Success }
                ?.widthDp
                ?: 0f

            BGEditHitEntry(
                topping = topping,
                painter = painter,
                target = with(density) {
                    ToppingHitTarget(
                        centerXPx = center.x.toPx(),
                        centerYPx = center.y.toPx(),
                        imageWidthPx = imageSize.width.toPx(),
                        imageHeightPx = imageSize.height.toPx(),
                        rotationDegrees = topping.rotationDegrees,
                        borderWidthPx = drawnBorderWidth.dp.toPx(),
                        mask = masks[topping.drawnModel],
                    )
                },
            )
        }
    }
}

/**
 * 캔버스 미리보기 박스 안, 저장된 배치([CanvasToppingItem.positionX]/[positionY])대로 겹쳐 그리는
 * 이미지. 캔버스 메인([CanvasToppingLayer])과 같은 규칙을 써야 편집한 그대로 돌아간다.
 *
 * [onClick]의 실제 동작(선택/선택 해제)은 호출하는 쪽에서 소유 여부에 따라 다르게 넘겨준다.
 * 선택 시 보이는 스트로크·버튼은 이 이미지와 함께 돌지 않아야 해서 [ToppingCornerButtons]에서
 * 별도로 그린다.
 */
@Composable
private fun CanvasToppingImage(
    entry: BGEditHitEntry,
    canvasWidth: Dp,
    canvasHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val size = with(density) {
        DpSize(entry.target.imageWidthPx.toDp(), entry.target.imageHeightPx.toDp())
    }
    val painterState by entry.painter.state.collectAsState()
    val border = entry.topping.borderLayers.firstOrNull()
    val description = stringResource(R.string.canvas_topping_content_description)

    Box(
        modifier = modifier
            .centeredAt(
                toppingCenter(
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    positionX = entry.topping.positionX,
                    positionY = entry.topping.positionY,
                ),
            ).requiredSize(size)
            .graphicsLayer(rotationZ = entry.topping.rotationDegrees)
            // 판정은 입력 레이어가 하지만, 접근성 서비스에는 토핑이 개별 버튼으로 보여야 한다
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
                onClick {
                    onClick()
                    true
                }
            },
    ) {
        YGToppingCutoutImage(
            painter = entry.painter,
            // 로딩·실패 상태에서 찍으면 플레이스홀더 실루엣이 테두리로 보인다
            borderColor = border
                ?.let { Color(it.colorArgb) }
                ?.takeIf { painterState is AsyncImagePainter.State.Success },
            borderWidth = (border?.widthDp ?: 0f).dp,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * 선택된 토핑의 스트로크와 그 네 모서리에 놓는 버튼들.
 * 좌측 상단=삭제, 우측 상단=회전(드래그 핸들), 좌측 하단=편집, 우측 하단=크기조절(드래그 핸들).
 */
@Composable
private fun ToppingCornerButtons(
    entry: BGEditHitEntry,
    canvasWidth: Dp,
    canvasHeight: Dp,
    onClickDelete: () -> Unit,
    onClickEdit: () -> Unit,
    onResize: (Float) -> Unit,
    onRotate: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val sizeAfterScale = with(density) {
        DpSize(entry.target.imageWidthPx.toDp(), entry.target.imageHeightPx.toDp())
    }
    val center = toppingCenter(
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        positionX = entry.topping.positionX,
        positionY = entry.topping.positionY,
    )
    val buttonPoints = computeToppingButtonPoints(
        center = center,
        sizeAfterScale = sizeAfterScale,
        rotationDegrees = entry.topping.rotationDegrees,
    )

    Box(modifier = modifier) {
        ToppingSelectionStroke(
            center = center,
            sizeAfterScale = sizeAfterScale,
            rotationDegrees = entry.topping.rotationDegrees,
        )
        YGCircleButton(
            iconResource = DesignSystemR.drawable.ic_close,
            type = YGCircleButtonType.Small,
            contentDescription = stringResource(R.string.canvas_bg_edit_topping_delete),
            onClick = onClickDelete,
            modifier = Modifier.centeredAt(buttonPoints.topLeft),
        )
        ToppingRotateHandleButton(
            iconRes = DesignSystemR.drawable.ic_rotate,
            contentDescription = stringResource(R.string.canvas_bg_edit_topping_rotate),
            point = buttonPoints.topRight,
            center = center,
            key = entry.topping.parfaitImageId,
            onRotate = onRotate,
        )
        YGCircleButton(
            iconResource = DesignSystemR.drawable.ic_edit,
            type = YGCircleButtonType.Small,
            contentDescription = stringResource(R.string.canvas_bg_edit_topping_edit),
            onClick = onClickEdit,
            modifier = Modifier.centeredAt(buttonPoints.bottomLeft),
        )
        ToppingResizeHandleButton(
            iconRes = DesignSystemR.drawable.ic_scale,
            contentDescription = stringResource(R.string.canvas_bg_edit_topping_resize),
            point = buttonPoints.bottomRight,
            center = center,
            key = entry.topping.parfaitImageId,
            onResize = onResize,
        )
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
        onToppingResize = {},
        onToppingRotate = {},
        onToppingMoveDrag = { _, _ -> },
        modifier = Modifier.fillMaxSize(),
    )
}
