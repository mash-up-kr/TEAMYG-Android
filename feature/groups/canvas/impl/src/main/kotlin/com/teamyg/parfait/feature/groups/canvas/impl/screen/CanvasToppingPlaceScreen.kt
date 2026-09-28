package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarEdit
import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.outline.ToppingOutlineCache
import com.teamyg.parfait.core.util.android.extension.centeredAt
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasToppingLayer
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingSelectionStroke
import com.teamyg.parfait.feature.groups.canvas.impl.component.rememberToppingBaseSize
import com.teamyg.parfait.feature.groups.canvas.impl.component.toppingTransformInput
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceUiState
import java.io.File

/**
 * 다듬기(영역/테두리 편집)를 마친 토핑 하나를 캔버스 위에 놓는 배치 화면.
 *
 * [CanvasBGEditScreen]의 토핑 탭과 UI가 비슷하지만, 이미 캔버스에 놓인 여러 토핑 중 하나를
 * 고르는 게 아니라 이제 막 편집을 마친 토핑 하나를 처음 배치하는 화면이라 더 단순하다 —
 * 탭 전환이 없고(하단 바 가운데는 고정 문구), 고를 대상도 하나뿐이라 탭해서 선택할 필요 없이
 * 처음부터 바로 두 손가락 제스처로 옮기고 크기·각도를 바꿀 수 있다(삭제·테두리 재편집 없음).
 */
@Composable
internal fun CanvasToppingPlaceScreen(
    uiState: CanvasToppingPlaceUiState,
    onClickClose: () -> Unit,
    onClickConfirm: () -> Unit,
    onToppingTransform: (pan: DpOffset, zoom: Float, rotationDelta: Float) -> Unit,
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
            // 초안은 절대경로를 담는다. Coil 에는 file 스킴 uri 로 바꿔 넘긴다
            val toppingImageModel = remember(toppingImagePath) {
                toppingImagePath?.let { path -> File(path).toUri().toString() }
            }
            val painter = rememberAsyncImagePainter(
                model = toppingImageModel,
                contentScale = ContentScale.Fit,
            )
            val painterState by painter.state.collectAsState()
            val isToppingImageLoaded = painterState is AsyncImagePainter.State.Success
            val baseSize = rememberToppingBaseSize(painter)

            val context = LocalContext.current
            // 초안이 비동기로 와서 첫 컴포지션의 모델이 언제나 null 이라, initialValue 를 한 번만
            // 읽는 produceState 로는 캐시를 못 쓴다
            var outline by remember(toppingImageModel) {
                mutableStateOf(toppingImageModel?.let { model -> ToppingOutlineCache.peek(model, retryKey = 0) })
            }

            LaunchedEffect(toppingImageModel) {
                val model = toppingImageModel ?: return@LaunchedEffect
                if (outline == null) outline = ToppingOutlineCache.load(context, model, retryKey = 0)
            }

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

            // 이미지·스트로크·핸들이 같은 자리에 오려면 셋이 같은 값을 봐야 한다. 여기서 한 번만 계산한다
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
                // 두 손가락은 캔버스 어디서든 시작해도 된다. 판정만 하는 빈 레이어라 형제 순서(그리는
                // 순서)는 화면에 보이지 않는다
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .toppingTransformInput(
                            targetAt = {
                                // 그림이 뜨기 전 center·sizeAfterScale 은 폴백 크기 기준이다. 이때 대상을
                                // 돌리면 두 손가락 제스처가 hasUserAdjustedPlacement 를 굳혀 initial
                                // placement(정중앙·기준 크기)가 영영 안 걸린다
                                if (!isToppingImageLoaded) {
                                    null
                                } else {
                                    with(density) {
                                        ToppingHitTarget(
                                            centerXPx = center.x.toPx(),
                                            centerYPx = center.y.toPx(),
                                            imageWidthPx = sizeAfterScale.width.toPx(),
                                            imageHeightPx = sizeAfterScale.height.toPx(),
                                            rotationDegrees = uiState.rotationDegrees,
                                            // 테두리를 그리지 않는 상태에서는 판정도 넓히지 않는다(CanvasToppingLayer와 같은 규칙)
                                            borderWidthPx = if (uiState.borderColorArgb != null) {
                                                (uiState.borderWidthDp ?: 0f).dp.toPx()
                                            } else {
                                                0f
                                            },
                                            outline = outline,
                                        )
                                    }
                                }
                            },
                            onTransform = { pan: Offset, zoom, rotationDelta ->
                                onToppingTransform(
                                    with(density) { DpOffset(pan.x.toDp(), pan.y.toDp()) },
                                    zoom,
                                    rotationDelta,
                                )
                            },
                        ),
                )

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
                    hitTestEnabled = false,
                    // 배경으로만 까는 자리다. 게이트를 켜면 배치 중인 토핑 옆에서 로딩이 돈다
                    revealTogether = false,
                    modifier = Modifier.fillMaxSize(),
                )

                // 캔버스(배경+기존 토핑) 전체를 딤 처리해, 지금 배치 중인 토핑만 도드라져 보이게 한다
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(YGAtomicColors.Transparency.Black25),
                )

                // Image()를 그냥 두면 painter.intrinsicSize로 스스로 크기를 맞춰(sizeToIntrinsics)
                // 스트로크·핸들 계산과 갈린다. 크기는 이 바깥 Box가 잡고 Image는 채우기만 한다
                Box(
                    modifier = Modifier
                        .centeredAt(center)
                        .requiredSize(sizeAfterScale)
                        .graphicsLayer(rotationZ = uiState.rotationDegrees),
                ) {
                    YGToppingCutoutImage(
                        painter = painter,
                        // 그림이 뜨기 전에 찍으면 플레이스홀더 실루엣이 테두리로 보인다
                        borderColor = uiState.borderColorArgb
                            ?.takeIf { isToppingImageLoaded }
                            ?.let { argb -> Color(argb) },
                        borderWidth = (uiState.borderWidthDp ?: 0f).dp,
                        modifier = Modifier.fillMaxSize(),
                        outline = outline,
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

@YGPreview
@Composable
private fun PreviewCanvasToppingPlaceScreen() = PreviewBox {
    CanvasToppingPlaceScreen(
        uiState = CanvasToppingPlaceUiState(),
        onClickClose = {},
        onClickConfirm = {},
        onToppingTransform = { _, _, _ -> },
        onCanvasMeasured = {},
        onToppingBaseSizeMeasured = {},
        onToppingImageReadyChanged = {},
        modifier = Modifier.fillMaxSize(),
    )
}
