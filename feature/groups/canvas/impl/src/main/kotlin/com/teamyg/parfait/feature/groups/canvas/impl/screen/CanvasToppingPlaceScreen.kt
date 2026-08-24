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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasToppingLayer
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingDragHandleButton
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingSelectionStroke
import com.teamyg.parfait.feature.groups.canvas.impl.component.rememberToppingBaseSize
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarEdit
import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO
import com.teamyg.parfait.core.util.android.extension.dragBy
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.util.computeToppingButtonPoints
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceUiState
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import java.io.File

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
    onToppingImageReadyChanged: (Boolean) -> Unit,
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
            val toppingImagePath = uiState.toppingImagePath
            val painter = rememberAsyncImagePainter(
                // 초안은 절대경로를 담는다. Coil 에는 file 스킴 uri 로 바꿔 넘긴다
                model = remember(toppingImagePath) {
                    toppingImagePath?.let { path -> File(path).toUri().toString() }
                },
                contentScale = ContentScale.Fit,
            )
            val painterState by painter.state.collectAsState()
            val isToppingImageLoaded = painterState is AsyncImagePainter.State.Success
            val baseSize = rememberToppingBaseSize(painter)

            // 확정 판정의 근거를 ViewModel 자기 어휘로 올린다 — 실측 방출 가드에 기대면
            // 그 가드를 걷는 순간 확인 버튼이 폴백 크기로 확정을 내보낸다
            LaunchedEffect(isToppingImageLoaded) {
                onToppingImageReadyChanged(isToppingImageLoaded)
            }

            // 그림이 뜨기 전 실측은 고정 폴백 크기다. 그것을 올려보내면 폴백 기준으로 계산된 배율이
            // 배치에 굳는다 — 초안을 읽어 오는 동안 그 창이 생긴다
            LaunchedEffect(baseSize, isToppingImageLoaded) {
                if (isToppingImageLoaded) onToppingBaseSizeMeasured(baseSize)
            }

            // 스트로크·핸들과 정확히 같은 자리·크기를 그리려면 이미지도 이 값을 그대로 써야 한다 —
            // 이미지는 graphicsLayer(scale), 스트로크는 requiredSize(sizeAfterScale)처럼
            // 서로 다른 방식으로 "같은 배율"을 표현하면, 그 둘이 실제로 같은 값을 내는지는
            // Compose 내부 구현에 기대는 셈이라 어긋나기 쉽다. 여기서 한 번만 계산해서 그대로 넘긴다.
            val center = DpOffset(
                x = uiState.offsetX + baseSize.width / 2,
                y = uiState.offsetY + baseSize.height / 2,
            )
            val sizeAfterScale = DpSize(baseSize.width * uiState.scale, baseSize.height * uiState.scale)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CANVAS_AREA_ASPECT_RATIO)
                    .onSizeChanged { size ->
                        with(density) {
                            onCanvasMeasured(DpSize(size.width.toDp(), size.height.toDp()))
                        }
                    }.clipToBounds()
                    .let { if (uiState.backgroundImageUrl == null) it.background(uiState.backgroundColor) else it }
                    .border(
                        width = 1.dp,
                        color = YGAtomicColors.Gray.Gray500,
                    ),
            ) {
                uiState.backgroundImageUrl?.let { imageUrl ->
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // 이미 캔버스에 놓인 토핑들. 지금 배치 중인 새 토핑과 같은 좌표계(Canvas-Area 대비 비율)다
                CanvasToppingLayer(
                    toppings = uiState.existingToppings,
                    spotlightedToppingId = null,
                    onClickTopping = {},
                    onClickSpotlightDim = {},
                    modifier = Modifier.fillMaxSize(),
                )

                // 캔버스(배경+기존 토핑) 전체를 딤 처리해, 지금 배치 중인 토핑만 도드라져 보이게 한다
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(YGAtomicColors.Transparency.Black25),
                )

                // Image()는 painter.intrinsicSize를 다시 읽어 스스로 크기를 맞추려 한다
                // (sizeToIntrinsics) — 그 시점이 requiredSize(sizeAfterScale)와 어긋나면 실제
                // 그려지는 크기가 스트로크·핸들 계산과 달라진다. 크기는 이 바깥 Box가 고정하고,
                // Image 자신은 그 Box를 꽉 채우기만 하도록 둬서 intrinsic 기반 자체 사이징을 막는다.
                Box(
                    modifier = Modifier
                        .offset(
                            x = center.x - sizeAfterScale.width / 2,
                            y = center.y - sizeAfterScale.height / 2,
                        ).requiredSize(sizeAfterScale)
                        .dragBy(Unit) { delta ->
                            onToppingMoveDrag(
                                with(density) { DpOffset(delta.x.toDp(), delta.y.toDp()) },
                            )
                        }.graphicsLayer(rotationZ = uiState.rotationDegrees),
                ) {
                    YGToppingCutoutImage(
                        painter = painter,
                        // 그림이 뜨기 전에 찍으면 플레이스홀더 실루엣이 테두리로 보인다
                        borderColor = uiState.borderColorArgb
                            ?.takeIf { isToppingImageLoaded }
                            ?.let { argb -> Color(argb) },
                        borderWidth = (uiState.borderWidthDp ?: 0f).dp,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // 스트로크는 토핑 이미지와 달리 캔버스를 넘어가도 잘리면 안 되고 진짜 크기 그대로 보여야 한다
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CANVAS_AREA_ASPECT_RATIO),
            ) {
                ToppingSelectionStroke(
                    center = center,
                    sizeAfterScale = sizeAfterScale,
                    rotationDegrees = uiState.rotationDegrees,
                )
            }

            // 버튼 좌표는 캔버스 밖으로 나간 진짜 모서리 값을 그대로 쓰되(clamp 없음),
            // 그려지는 픽셀은 토핑 이미지와 마찬가지로 캔버스 경계에서 잘리게 한다
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CANVAS_AREA_ASPECT_RATIO)
                    .clipToBounds(),
            ) {
                ToppingPlaceCornerButtons(
                    center = center,
                    sizeAfterScale = sizeAfterScale,
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

/** 배치 중인 토핑의 리사이즈·회전 두 모서리 버튼만 그린다(삭제·편집 없음). */
@Composable
private fun ToppingPlaceCornerButtons(
    center: DpOffset,
    sizeAfterScale: DpSize,
    rotationDegrees: Float,
    onResizeDrag: (Offset) -> Unit,
    onRotateDrag: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonPoints = computeToppingButtonPoints(
        center = center,
        sizeAfterScale = sizeAfterScale,
        rotationDegrees = rotationDegrees,
    )

    Box(modifier = modifier) {
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
        uiState = CanvasToppingPlaceUiState(),
        onClickClose = {},
        onClickConfirm = {},
        onToppingMoveDrag = {},
        onToppingResizeDrag = {},
        onToppingRotateDrag = {},
        onCanvasMeasured = {},
        onToppingBaseSizeMeasured = {},
        onToppingImageReadyChanged = {},
        modifier = Modifier.fillMaxSize(),
    )
}
