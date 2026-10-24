package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.lerp
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarTitle
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarTitleConfirm
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasTextInputToolbar
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasToppingLayer
import com.teamyg.parfait.feature.groups.canvas.impl.component.TextColorChipRow
import com.teamyg.parfait.feature.groups.canvas.impl.component.TextToppingBackground
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingArrangeCanvasDim
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingArrangeCanvasSurface
import com.teamyg.parfait.feature.groups.canvas.impl.component.editingTopAnchored
import com.teamyg.parfait.feature.groups.canvas.impl.component.scaleBoxToFit
import com.teamyg.parfait.feature.groups.canvas.impl.component.textEditDragInput
import com.teamyg.parfait.feature.groups.canvas.impl.component.toppingTapInput
import com.teamyg.parfait.feature.groups.canvas.impl.component.toppingTransformInput
import com.teamyg.parfait.feature.groups.canvas.impl.component.withTextToppingSizing
import com.teamyg.parfait.feature.groups.canvas.impl.util.TEXT_TOPPING_FONT_SIZE_SP
import com.teamyg.parfait.feature.groups.canvas.impl.util.TEXT_TOPPING_MAX_LENGTH
import com.teamyg.parfait.feature.groups.canvas.impl.util.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.util.animatePanelFocusFraction
import com.teamyg.parfait.feature.groups.canvas.impl.util.detectTextToppingScript
import com.teamyg.parfait.feature.groups.canvas.impl.util.rememberDedupeHangulEnterTransformation
import com.teamyg.parfait.feature.groups.canvas.impl.util.rememberMaxLengthInputTransformation
import com.teamyg.parfait.feature.groups.canvas.impl.util.rememberTextEditModeState
import com.teamyg.parfait.feature.groups.canvas.impl.util.rememberTextEditVerticalAnchor
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasTextInputUiState

/** 글자 가로 최대(글자만 기준, 배경 여백 불포함) */
private val TEXT_TOPPING_MAX_TEXT_WIDTH = 335.dp

// 텍스트 재탭·드래그 판정 영역
private val TEXT_TOPPING_MIN_TOUCH = 44.dp

@Composable
internal fun CanvasTextInputScreen(
    uiState: CanvasTextInputUiState,
    onContentChange: (String) -> Unit,
    onSelectColor: (TextToppingColor) -> Unit,
    onToggleColorPopover: () -> Unit,
    onClickType: () -> Unit,
    onClickAlign: () -> Unit,
    onCanvasMeasured: (DpSize) -> Unit,
    onTextBoxMeasured: (DpSize) -> Unit,
    onToppingTransform: (pan: DpOffset, zoom: Float, rotationDelta: Float) -> Unit,
    onDragPlacementSeed: (offsetX: Dp, offsetY: Dp) -> Unit,
    onClickConfirm: () -> Unit,
    onClickClose: () -> Unit,
    onClickStack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val dedupeHangulEnterTransformation = rememberDedupeHangulEnterTransformation()
    val maxLengthTransformation = rememberMaxLengthInputTransformation()
    val textFieldState = rememberTextFieldState(initialText = uiState.content)
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { content -> onContentChange(content) }
    }

    val textEditMode = rememberTextEditModeState(
        isContentBlank = uiState.content.isBlank(),
        onConfirmEmpty = onClickConfirm,
    )

    // 화면(root) 전체 높이. Text-Area 세로 고정 계산에 쓴다
    var rootHeightPx by remember { mutableIntStateOf(0) }
    // 캔버스가 화면(root) 안에서 시작하는 y — Text-Area 세로 고정 계산에 쓴다
    var canvasTopPx by remember { mutableFloatStateOf(0f) }

    val editAreaHeightPx = with(density) { (uiState.canvasSize?.height ?: 0.dp).roundToPx() }
    val textEditAnchor = rememberTextEditVerticalAnchor(
        density = density,
        isEditing = textEditMode.isEditing,
        canvasTopPx = canvasTopPx,
        rootHeightPx = rootHeightPx,
        editAreaHeightPx = editAreaHeightPx,
        textBoxHeightDp = uiState.textBoxSize?.height,
    )

    // 박스가 세로 예산을 넘으면 graphicsLayer 로 비율 축소
    val fontSizeSp = TEXT_TOPPING_FONT_SIZE_SP
    val placeholderText = stringResource(R.string.canvas_text_input_placeholder)

    // 입력 언어로 글꼴·자간을 통째 고른다(일→Line Seed JP, 중→TW/자간0%, 그 외→SUIT/자간-1.1%).
    val textScript = remember(uiState.content) { detectTextToppingScript(uiState.content) }
    val autoSizeBaseStyle = YGTheme.typography.body.b01R.copy(
        fontFamily = textScript.fontFamily,
        letterSpacing = textScript.letterSpacingEm.em,
    )
    // 글자 래핑 폭(최대 335dp, 캔버스 폭에서 좌우 여백을 뺀 값으로 제한).
    val editSideMargin = YGTheme.layout.padding.padding2
    val textMaxWidthDp = uiState.canvasSize
        ?.let { (it.width - editSideMargin * 2).coerceAtMost(TEXT_TOPPING_MAX_TEXT_WIDTH) }
        ?: TEXT_TOPPING_MAX_TEXT_WIDTH

    // 직접 재서 폭을 명시적으로 고정한다.
    val textMeasurer = rememberTextMeasurer()
    val hugRenderStyle = autoSizeBaseStyle.withTextToppingSizing(fontSizeSp)
    val hugMeasureText = textFieldState.text.toString().ifEmpty { placeholderText }
    val hugWidthDp = remember(hugMeasureText, hugRenderStyle, textMaxWidthDp, density) {
        val naturalWidthPx = textMeasurer.measure(text = hugMeasureText, style = hugRenderStyle).size.width
        with(density) { naturalWidthPx.toDp() }.coerceAtMost(textMaxWidthDp)
    }

    val focusFraction by animatePanelFocusFraction(textEditMode.isEditing)
    val storedCenter = DpOffset(
        x = uiState.offsetX + (uiState.textBoxSize?.width ?: 0.dp) / 2,
        y = uiState.offsetY + (uiState.textBoxSize?.height ?: 0.dp) / 2,
    )
    val canvasWidthPx = with(density) { (uiState.canvasSize?.width ?: 0.dp).roundToPx() }

    // 드래그·히트 판정이 쓰는 "현재 그려진 중심". x 는 캔버스 중앙, y 는 윗변 + 박스높이/2
    val editingCenter = uiState.canvasSize?.let { canvasSize ->
        with(density) {
            DpOffset(x = canvasSize.width / 2, y = (textEditAnchor.topPx + textEditAnchor.heightPx / 2f).toDp())
        }
    } ?: storedCenter
    val drawnCenter = lerp(storedCenter, editingCenter, focusFraction)
    // 편집 중엔 처음 작성할 때처럼 배율·회전 없이 보여야 한다
    val drawnScale = uiState.scale + (1f - uiState.scale) * focusFraction
    val drawnRotationDegrees = uiState.rotationDegrees * (1f - focusFraction)

    // 키보드 ↑ 상태에서 텍스트를 끌면: 점프 없이 지금 자리를 배치 좌표로 심고 → 키보드를 내려
    // 배치로 전환 → 이후 이동을 이어 간다(스펙 "놓은 곳이 새 위치"). 빈 내용은 끌기 불가
    val onEditDragStart: () -> Unit = {
        uiState.textBoxSize?.let { box ->
            onDragPlacementSeed(drawnCenter.x - box.width / 2, drawnCenter.y - box.height / 2)
        }
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    val onEditDrag: (Offset) -> Unit = { pan ->
        onToppingTransform(with(density) { DpOffset(pan.x.toDp(), pan.y.toDp()) }, 1f, 0f)
    }

    // 텍스트는 실루엣이 없어 outline = null 로 넘긴다
    val hitTarget: () -> ToppingHitTarget? = {
        uiState.textBoxSize?.let { size ->
            with(density) {
                val minTouchPx = TEXT_TOPPING_MIN_TOUCH.toPx()
                ToppingHitTarget(
                    centerXPx = drawnCenter.x.toPx(),
                    centerYPx = drawnCenter.y.toPx(),
                    imageWidthPx = maxOf((size.width * uiState.scale).toPx(), minTouchPx),
                    imageHeightPx = maxOf((size.height * uiState.scale).toPx(), minTouchPx),
                    rotationDegrees = uiState.rotationDegrees,
                    borderWidthPx = 0f,
                    outline = null,
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size -> rootHeightPx = size.height },
    ) {
        if (textEditMode.isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(YGAtomicColors.Transparency.Black25),
            )
        }

        // 캔버스를 그대로 가져온다
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = YGTheme.layout.padding.padding7),
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(CANVAS_AREA_ASPECT_RATIO)
                    .let { if (textEditMode.isEditing) it else it.clipToBounds() },
            ) {
                ToppingArrangeCanvasSurface(
                    backgroundColor = uiState.backgroundColor,
                    backgroundImageUrl = uiState.backgroundImageUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            canvasTopPx = coordinates.boundsInRoot().top
                        }.onSizeChanged { size ->
                            with(density) {
                                onCanvasMeasured(DpSize(size.width.toDp(), size.height.toDp()))
                            }
                        },
                ) {
                    // 이미 캔버스에 놓인 토핑들. 터치 판정은 안 받는다 — 판정은 아래 입력 레이어가 혼자 한다
                    CanvasToppingLayer(
                        toppings = uiState.existingToppings,
                        spotlightedToppingId = null,
                        onClickTopping = {},
                        onClickSpotlightDim = {},
                        hitTestEnabled = false,
                        revealTogether = false,
                        modifier = Modifier.fillMaxSize(),
                    )

                    // 편집 중에만 캔버스(배경·기존 토핑)를 눌러 둔다 — 다른 화면과 같은 딤 컴포넌트.
                    // 배치 모드는 실제 캔버스가 그대로 보여야 한다
                    if (textEditMode.isEditing) {
                        ToppingArrangeCanvasDim()
                    }

                    // 편집 중 빈 곳 탭 — 텍스트 **뒤**에 깐다. 텍스트 자체를 누르면 필드(커서)가
                    // 받고, 바깥을 누르면 이 레이어가 받아 키보드를 내린다
                    if (textEditMode.isEditing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickableYGNoRipple {
                                    if (uiState.isColorPopoverOpen) {
                                        onToggleColorPopover()
                                    } else {
                                        textEditMode.endEditing()
                                    }
                                },
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .editingTopAnchored(
                            arrangeCenter = storedCenter,
                            editTopPx = textEditAnchor.topPx,
                            canvasWidthPx = canvasWidthPx,
                            focusFraction = focusFraction,
                        ).textEditDragInput(
                            enabled = { textEditMode.isEditing && uiState.content.isNotBlank() },
                            onDragStart = onEditDragStart,
                            onDrag = onEditDrag,
                        ).scaleBoxToFit(
                            maxWidthPx = if (textEditMode.isEditing) 0 else canvasWidthPx,
                            maxHeightPx = if (textEditMode.isEditing) textEditAnchor.budgetPx else editAreaHeightPx,
                        ).onSizeChanged { naturalSize ->
                            with(density) {
                                onTextBoxMeasured(DpSize(naturalSize.width.toDp(), naturalSize.height.toDp()))
                            }
                            textEditAnchor.onMeasured()
                        }.graphicsLayer(
                            scaleX = drawnScale,
                            scaleY = drawnScale,
                            rotationZ = drawnRotationDegrees,
                        ).let {
                            // 편집 중: 아직 폰트 자동축소가 안 걸린 동안에만 윗변과 같은 리듬으로 아래쪽을 가려 두다가 드러낸다.
                            if (textEditMode.isEditing && textEditAnchor.targetHeightPx <= textEditAnchor.budgetPx) {
                                it.drawWithContent {
                                    clipRect(
                                        left = 0f,
                                        top = 0f,
                                        right = size.width,
                                        bottom = textEditAnchor.heightPx,
                                    ) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                            } else {
                                it
                            }
                        },
                ) {
                    TextToppingBackground(
                        type = uiState.textType,
                        color = uiState.selectedColor,
                        fontSizeSp = fontSizeSp,
                    ) {
                        // 포커스 유무가 곧 편집/배치다(클래스 KDoc 참고)
                        BasicTextField(
                            state = textFieldState,
                            inputTransformation = dedupeHangulEnterTransformation.then(maxLengthTransformation),
                            textStyle = autoSizeBaseStyle
                                .withTextToppingSizing(fontSizeSp)
                                .copy(
                                    color = uiState.selectedColor.contentColor,
                                    textAlign = uiState.align,
                                ),
                            modifier = Modifier
                                // hugWidthDp 가 이미 335dp 캡과 실측 폭 중 작은 쪽이라 hug 와 랩 캡을
                                // 둘 다 만족한다(위 hugWidthDp 계산 참고)
                                .width(hugWidthDp)
                                .focusRequester(textEditMode.focusRequester)
                                // 포커스 유무만 모드에 반영한다. "빈 내용이면 취소"는 의도한 종료
                                .onFocusChanged { focusState -> textEditMode.onFocusChanged(focusState.isFocused) },
                            // 빈 상태 플레이스홀더. 글자 크기·정렬은 본문과 동일
                            decorator = { innerTextField ->
                                if (textFieldState.text.isEmpty()) {
                                    Text(
                                        text = placeholderText,
                                        style = autoSizeBaseStyle
                                            .withTextToppingSizing(fontSizeSp)
                                            .copy(
                                                color = YGAtomicColors.Transparency.White75,
                                                textAlign = uiState.align,
                                            ),
                                    )
                                }
                                innerTextField()
                            },
                        )
                    }
                }

                // 배치 모드의 탭·팬·핀치는 텍스트 **위** 전면 오버레이가 독점한다. 텍스트와 같은 이유로
                // 캔버스 밖(형제)에 둬야 z-order(텍스트 위)가 유지된다
                if (!textEditMode.isEditing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .toppingTapInput(
                                entries = { listOfNotNull(hitTarget()?.let { target -> Unit to target }) },
                                keyOf = { it },
                                onHit = { textEditMode.resumeEditing() },
                                onMiss = {},
                            ).toppingTransformInput(
                                targetAt = hitTarget,
                                onTransform = { pan, zoom, rotationDelta ->
                                    onToppingTransform(
                                        with(density) { DpOffset(pan.x.toDp(), pan.y.toDp()) },
                                        zoom,
                                        rotationDelta,
                                    )
                                },
                            ),
                    )
                }
            }
        }

        // 헤더
        if (textEditMode.isEditing) {
            YGFloatingBarTitleConfirm(
                title = stringResource(R.string.canvas_text_input_title),
                onConfirmClick = textEditMode.endEditing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .fillMaxWidth(),
            )
        } else {
            YGFloatingBarTitle(
                title = stringResource(R.string.canvas_text_input_title),
                onCloseClick = onClickClose,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .fillMaxWidth(),
            )
        }

        // 편집 모드: 색상바·툴바만 키보드 위에 뜬다.
        if (textEditMode.isEditing) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .clickableYGNoRipple {},
            ) {
                // 열려 있는 동안 Text-Area 아래쪽을 잠깐 가린다 — 텍스트 자리 자체는 바뀌지 않는다
                if (uiState.isColorPopoverOpen) {
                    TextColorChipRow(
                        selectedColor = uiState.selectedColor,
                        onSelectColor = onSelectColor,
                        contentPadding = PaddingValues(horizontal = YGTheme.layout.padding.padding8, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(YGAtomicColors.Transparency.White50),
                    )
                }

                CanvasTextInputToolbar(
                    selectedColor = uiState.selectedColor,
                    textType = uiState.textType,
                    align = uiState.align,
                    characterCount = uiState.characterCount,
                    isAtLimit = uiState.isAtLimit,
                    onClickColor = onToggleColorPopover,
                    onClickType = onClickType,
                    onClickAlign = onClickAlign,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(YGAtomicColors.Transparency.White75),
                )
            }
        } else {
            YGButton(
                text = stringResource(R.string.canvas_topping_arrange_confirm),
                buttonType = YGButtonType.Large,
                isEnabled = true,
                onClick = onClickStack,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = YGTheme.layout.padding.padding7)
                    .padding(bottom = YGTheme.layout.padding.padding1),
            )
        }
    }
}

@YGPreview
@Composable
private fun PreviewCanvasTextInputScreen() = PreviewBox {
    CanvasTextInputScreen(
        uiState = CanvasTextInputUiState(content = "오늘 다 같이 먹은 파르페"),
        onContentChange = {},
        onSelectColor = {},
        onToggleColorPopover = {},
        onClickType = {},
        onClickAlign = {},
        onCanvasMeasured = {},
        onTextBoxMeasured = {},
        onToppingTransform = { _, _, _ -> },
        onDragPlacementSeed = { _, _ -> },
        onClickConfirm = {},
        onClickClose = {},
        onClickStack = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@YGPreview
@Composable
private fun PreviewCanvasTextInputScreenAtLimit() = PreviewBox {
    CanvasTextInputScreen(
        uiState = CanvasTextInputUiState(
            content = "가".repeat(TEXT_TOPPING_MAX_LENGTH),
            selectedColor = TextToppingColor.Violet,
        ),
        onContentChange = {},
        onSelectColor = {},
        onToggleColorPopover = {},
        onClickType = {},
        onClickAlign = {},
        onCanvasMeasured = {},
        onTextBoxMeasured = {},
        onToppingTransform = { _, _, _ -> },
        onDragPlacementSeed = { _, _ -> },
        onClickConfirm = {},
        onClickClose = {},
        onClickStack = {},
        modifier = Modifier.fillMaxSize(),
    )
}
