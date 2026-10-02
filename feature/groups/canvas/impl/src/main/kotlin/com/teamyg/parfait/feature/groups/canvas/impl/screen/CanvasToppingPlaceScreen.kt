package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.core.net.toUri
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarBackTitleClose
import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.outline.ToppingOutlineCache
import com.teamyg.parfait.core.util.android.extension.centeredAt
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasToppingLayer
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingArrangeLayout
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingBorderPanel
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingSelectionStroke
import com.teamyg.parfait.feature.groups.canvas.impl.component.dismissPanelOnTouch
import com.teamyg.parfait.feature.groups.canvas.impl.component.rememberToppingBaseSize
import com.teamyg.parfait.feature.groups.canvas.impl.component.toppingTapInput
import com.teamyg.parfait.feature.groups.canvas.impl.component.toppingTransformInput
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BORDER_WIDTH_RANGE_DP
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.util.panelFocusCenter
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceUiState
import java.io.File
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

private const val INPUT_TAG = "topping_place_input"

/**
 * 누끼를 딴 토핑 하나를 캔버스 위에 놓고 테두리를 정하는 배치 화면.
 *
 * 고를 대상이 하나뿐이라 선택 단계 없이 처음부터 바로 옮기고 크기·각도를 바꿀 수 있다. 토핑을
 * 탭하면 테두리 패널이 열리고, 열린 동안에는 토핑이 패널에 가리지 않는 자리로 옮겨 보인다 —
 * 보이는 자리만 바뀌고 [CanvasToppingPlaceUiState] 의 위치는 그대로다.
 */
@Composable
internal fun CanvasToppingPlaceScreen(
    uiState: CanvasToppingPlaceUiState,
    onClickBack: () -> Unit,
    onClickClose: () -> Unit,
    onClickConfirm: () -> Unit,
    onClickTopping: () -> Unit,
    onToggleBorderPanel: () -> Unit,
    onDismissBorderPanel: () -> Unit,
    onSelectBorderColor: (Int?) -> Unit,
    onChangeBorderWidth: (Float) -> Unit,
    onToppingTransform: (pan: DpOffset, zoom: Float, rotationDelta: Float) -> Unit,
    onCanvasMeasured: (DpSize) -> Unit,
    onToppingBaseSizeMeasured: (DpSize) -> Unit,
    onToppingImageReadyChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

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

    // 저장된 자리. 터치 판정은 이것으로 한다
    val center = DpOffset(
        x = uiState.offsetX + baseSize.width / 2,
        y = uiState.offsetY + baseSize.height / 2,
    )
    val sizeAfterScale = DpSize(baseSize.width * uiState.scale, baseSize.height * uiState.scale)

    // 그리는 자리. 이미지와 스트로크가 같은 자리에 오려면 같은 값을 봐야 한다
    val focusFraction by animateFloatAsState(
        targetValue = if (uiState.isBorderPanelOpen) 1f else 0f,
        label = "toppingPanelFocus",
    )
    val drawnCenter = uiState.canvasSize
        ?.let { canvasSize -> lerp(center, panelFocusCenter(canvasSize), focusFraction) }
        ?: center

    val border = uiState.border

    // 그림이 뜨기 전 크기는 폴백이라, 그때 제스처를 받으면 초기 배치가 영영 안 걸린다.
    // 지역 함수로 두고 `::hitTarget` 으로 넘기지 않는다 — 그 참조는 첫 컴포지션의 값을 붙잡은 채
    // 갱신되지 않아서 그림이 뜬 뒤에도 대상이 없다고 답한다
    val hitTarget: () -> ToppingHitTarget? = {
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
                    // 테두리를 안 그리면 판정도 넓히지 않는다
                    borderWidthPx = border?.widthDp?.dp?.toPx() ?: 0f,
                    outline = outline,
                )
            }
        }
    }

    ToppingArrangeLayout(
        header = {
            YGFloatingBarBackTitleClose(
                title = stringResource(R.string.canvas_topping_place_title),
                onBackClick = onClickBack,
                onCloseClick = onClickClose,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        onClickConfirm = onClickConfirm,
        panel = {
            ToppingBorderPanel(
                isOpen = uiState.isBorderPanelOpen,
                selectedColorArgb = border?.colorArgb,
                widthDp = uiState.panelBorderWidthDp,
                widthRange = TOPPING_BORDER_WIDTH_RANGE_DP,
                onClickToggle = onToggleBorderPanel,
                onSelectColor = onSelectBorderColor,
                onChangeWidth = onChangeBorderWidth,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
        },
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
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
            // 스트로크 계산과 갈린다. 크기는 이 바깥 Box가 잡고 Image는 채우기만 한다
            Box(
                modifier = Modifier
                    .centeredAt(drawnCenter)
                    .requiredSize(sizeAfterScale)
                    .graphicsLayer(rotationZ = uiState.rotationDegrees),
            ) {
                YGToppingCutoutImage(
                    painter = painter,
                    // 그림이 뜨기 전에 찍으면 플레이스홀더 실루엣이 테두리로 보인다
                    borderColor = border
                        ?.takeIf { isToppingImageLoaded }
                        ?.let { style -> Color(style.colorArgb) },
                    borderWidth = (border?.widthDp ?: 0f).dp,
                    modifier = Modifier.fillMaxSize(),
                    outline = outline,
                )
            }
        }

        // 스트로크는 토핑 이미지와 달리 캔버스를 넘어가도 잘리면 안 되고 진짜 크기 그대로 보여야 한다
        Box(modifier = Modifier.matchParentSize()) {
            ToppingSelectionStroke(
                center = drawnCenter,
                sizeAfterScale = sizeAfterScale,
                rotationDegrees = uiState.rotationDegrees,
            )
        }

        // 입력은 패널의 형제 자리에 둔다. 패널의 조상에 달면 패널 빈 곳을 누른 터치까지 보게 돼
        // 패널이 닫힌다. 세 입력의 순서는 dismissPanelOnTouch KDoc 참고
        Box(
            modifier = Modifier
                .matchParentSize()
                .testTag(INPUT_TAG)
                .dismissPanelOnTouch(
                    isPanelOpen = { uiState.isBorderPanelOpen },
                    onDismiss = onDismissBorderPanel,
                ).toppingTapInput(
                    entries = { listOfNotNull(hitTarget()?.let { target -> Unit to target }) },
                    keyOf = { it },
                    onHit = { onClickTopping() },
                    onMiss = {},
                    enabled = { !uiState.isBorderPanelOpen },
                ).toppingTransformInput(
                    targetAt = hitTarget,
                    onTransform = { pan: Offset, zoom, rotationDelta ->
                        onToppingTransform(
                            with(density) { DpOffset(pan.x.toDp(), pan.y.toDp()) },
                            zoom,
                            rotationDelta,
                        )
                    },
                    enabled = { !uiState.isBorderPanelOpen },
                ),
        )
    }
}

@Composable
internal fun ToppingPlaceQuitDialog(
    onConfirmQuit: () -> Unit,
    onDismiss: () -> Unit,
) {
    YGModalPopup(
        title = stringResource(R.string.canvas_topping_place_quit_dialog_title),
        body = stringResource(R.string.canvas_topping_place_quit_dialog_body),
        iconRes = DesignSystemR.drawable.ic_warning_round,
        secondaryText = stringResource(R.string.canvas_topping_place_quit_dialog_confirm),
        onSecondaryClick = onConfirmQuit,
        primaryText = stringResource(R.string.canvas_topping_place_quit_dialog_cancel),
        onPrimaryClick = onDismiss,
        onDismissRequest = onDismiss,
    )
}

private class CanvasToppingPlacePreviewParameterProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(false, true)
}

@YGPreview
@Composable
private fun PreviewCanvasToppingPlaceScreen(
    @PreviewParameter(CanvasToppingPlacePreviewParameterProvider::class) isBorderPanelOpen: Boolean,
) = PreviewBox {
    CanvasToppingPlaceScreen(
        uiState = CanvasToppingPlaceUiState(isBorderPanelOpen = isBorderPanelOpen),
        onClickBack = {},
        onClickClose = {},
        onClickConfirm = {},
        onClickTopping = {},
        onToggleBorderPanel = {},
        onDismissBorderPanel = {},
        onSelectBorderColor = {},
        onChangeBorderWidth = {},
        onToppingTransform = { _, _, _ -> },
        onCanvasMeasured = {},
        onToppingBaseSizeMeasured = {},
        onToppingImageReadyChanged = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@YGPreview
@Composable
private fun PreviewToppingPlaceQuitDialog() = PreviewBox {
    ToppingPlaceQuitDialog(onConfirmQuit = {}, onDismiss = {})
}
