package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarEdit
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.extension.dragBy
import com.teamyg.parfait.domain.model.CANVAS_ASPECT_RATIO
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.util.computeToppingButtonPoints
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceUiState
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/**
 * 다듬기(영역/테두리 편집)를 마친 토핑 하나를 캔버스 위에 놓는 배치 화면.
 *
 * [CanvasBGEditScreen]의 토핑 탭과 UI가 비슷하지만, 이미 캔버스에 놓인 여러 토핑 중 하나를
 * 고르는 게 아니라 이제 막 편집을 마친 토핑 하나를 처음 배치하는 화면이라 더 단순하다 —
 * 탭 전환이 없고(하단 바 가운데는 고정 문구), 고를 대상도 하나뿐이라 탭해서 선택할 필요 없이
 * 처음부터 바로 드래그해서 옮기고, 리사이즈·회전만 가능하다(삭제·테두리 재편집 없음).
 */
@Composable
internal fun CanvasToppingPlaceScreen(
    uiState: CanvasToppingPlaceUiState,
    onClickClose: () -> Unit,
    onClickConfirm: () -> Unit,
    onToppingMoveDrag: (DpOffset) -> Unit,
    onToppingResizeDrag: (Offset) -> Unit,
    onToppingRotateDrag: (Offset) -> Unit,
    onCanvasMeasured: (DpSize) -> Unit,
    onToppingBaseSizeMeasured: (DpSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    top = 60.dp,
                    bottom = 14.dp,
                    start = YGTheme.layout.padding.padding7,
                    end = YGTheme.layout.padding.padding7,
                ), // 60.dp/14.dp 공통에 없음
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CANVAS_ASPECT_RATIO)
                    .onSizeChanged { size ->
                        with(density) {
                            onCanvasMeasured(DpSize(size.width.toDp(), size.height.toDp()))
                        }
                    }.clipToBounds()
                    .background(uiState.backgroundColor)
                    .border(
                        width = 1.dp,
                        color = YGAtomicColors.Gray.Gray500,
                    ),
            ) {
                // 캔버스(배경) 전체를 딤 처리해, 지금 배치 중인 토핑만 도드라져 보이게 한다
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(YGAtomicColors.Transparency.Black25),
                )

                val painter = rememberAsyncImagePainter(model = uiState.toppingImageUri)
                val baseSize = rememberToppingBaseSize(painter)
                LaunchedEffect(baseSize) { onToppingBaseSizeMeasured(baseSize) }

                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .offset(x = uiState.offsetX, y = uiState.offsetY)
                        .size(baseSize)
                        .dragBy(Unit) { delta ->
                            onToppingMoveDrag(
                                with(density) { DpOffset(delta.x.toDp(), delta.y.toDp()) },
                            )
                        }.graphicsLayer(
                            scaleX = uiState.scale,
                            scaleY = uiState.scale,
                            rotationZ = uiState.rotationDegrees,
                        ),
                )

                ToppingPlaceCornerButtons(
                    offsetX = uiState.offsetX,
                    offsetY = uiState.offsetY,
                    baseSize = baseSize,
                    scale = uiState.scale,
                    rotationDegrees = uiState.rotationDegrees,
                    onResizeDrag = onToppingResizeDrag,
                    onRotateDrag = onToppingRotateDrag,
                )
            }
        }

        YGFloatingBarEdit(
            title = stringResource(R.string.canvas_topping_place_title),
            onCloseClick = onClickClose,
            onConfirmClick = onClickConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = YGTheme.layout.padding.padding6,
                    bottom = YGTheme.layout.padding.padding1,
                ),
        )
    }
}

/** 배치 중인 토핑의 스트로크와, 리사이즈·회전 두 모서리 버튼만 그린다(삭제·편집 없음). */
@Composable
private fun ToppingPlaceCornerButtons(
    offsetX: Dp,
    offsetY: Dp,
    baseSize: DpSize,
    scale: Float,
    rotationDegrees: Float,
    onResizeDrag: (Offset) -> Unit,
    onRotateDrag: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val center = DpOffset(
        x = offsetX + baseSize.width / 2,
        y = offsetY + baseSize.height / 2,
    )
    val sizeAfterScale = DpSize(baseSize.width * scale, baseSize.height * scale)
    val buttonPoints = computeToppingButtonPoints(
        center = center,
        sizeAfterScale = sizeAfterScale,
        rotationDegrees = rotationDegrees,
    )

    Box(modifier = modifier) {
        ToppingSelectionStroke(
            center = center,
            sizeAfterScale = sizeAfterScale,
            rotationDegrees = rotationDegrees,
        )
        ToppingDragHandleButton(
            iconRes = DesignSystemR.drawable.ic_scale,
            contentDescription = stringResource(R.string.canvas_bg_edit_topping_resize),
            point = buttonPoints.topRight,
            key = Unit,
            onDrag = onResizeDrag,
        )
        ToppingDragHandleButton(
            iconRes = DesignSystemR.drawable.ic_rotate,
            contentDescription = stringResource(R.string.canvas_bg_edit_topping_rotate),
            point = buttonPoints.bottomRight,
            key = Unit,
            onDrag = onRotateDrag,
        )
    }
}

@YGPreview
@Composable
private fun PreviewCanvasToppingPlaceScreen() = PreviewBox {
    CanvasToppingPlaceScreen(
        uiState = CanvasToppingPlaceUiState(toppingImageUri = ""),
        onClickClose = {},
        onClickConfirm = {},
        onToppingMoveDrag = {},
        onToppingResizeDrag = {},
        onToppingRotateDrag = {},
        onCanvasMeasured = {},
        onToppingBaseSizeMeasured = {},
        modifier = Modifier.fillMaxSize(),
    )
}
