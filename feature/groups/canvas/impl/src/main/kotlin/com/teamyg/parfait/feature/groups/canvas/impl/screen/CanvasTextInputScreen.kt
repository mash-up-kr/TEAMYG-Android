package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
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
import com.teamyg.parfait.core.util.android.extension.centeredAt
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasTextInputToolbar
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasToppingLayer
import com.teamyg.parfait.feature.groups.canvas.impl.component.TextColorChipRow
import com.teamyg.parfait.feature.groups.canvas.impl.component.TextToppingBackground
import com.teamyg.parfait.feature.groups.canvas.impl.component.textEditDragInput
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingArrangeCanvasDim
import com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingArrangeCanvasSurface
import com.teamyg.parfait.feature.groups.canvas.impl.component.toppingTapInput
import com.teamyg.parfait.feature.groups.canvas.impl.component.toppingTransformInput
import com.teamyg.parfait.feature.groups.canvas.impl.util.TEXT_TOPPING_FONT_SIZE_SP
import com.teamyg.parfait.feature.groups.canvas.impl.util.TEXT_TOPPING_MAX_LENGTH
import com.teamyg.parfait.feature.groups.canvas.impl.util.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.util.animatePanelFocusFraction
import com.teamyg.parfait.feature.groups.canvas.impl.util.detectTextToppingScript
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasTextInputUiState
import kotlinx.coroutines.delay

/** 입력 상태바(52dp hug)의 공칭 높이 — 편집 중 텍스트를 키보드 위 어디에 둘지 계산할 때만 쓴다 */
private val TEXT_TOPPING_TOOLBAR_HEIGHT = 52.dp

/** 빈 상태 플레이스홀더를 선택 색의 흐린 힌트로 보여 줄 때의 불투명도 */
private const val TEXT_TOPPING_PLACEHOLDER_ALPHA = 0.5f

/** 자동 크기: 글자 가로 최대(글자만 기준, 배경 여백 불포함) */
private val TEXT_TOPPING_MAX_TEXT_WIDTH = 335.dp

/** 자동 크기: 하한은 없지만(스펙) 0 과 무한 루프를 막는 바닥값 */
private const val TEXT_TOPPING_MIN_FONT_SIZE_SP = 0.5f

/** 자동 크기 이진 탐색 반복 수 — 24sp 범위를 2^10 로 쪼개 약 0.02sp 정밀도 */
private const val AUTO_SIZE_SEARCH_ITERATIONS = 10

/** 자동 크기 재계산 디바운스(ms) — 타이핑·키보드 애니메이션이 멈춘 뒤에만 다시 잰다 */
private const val AUTO_SIZE_DEBOUNCE_MS = 50L

/** 글자가 아주 작아져도 배치 모드 재탭·드래그 판정 영역은 이보다 작아지지 않는다(시각 크기는 그대로) */
private val TEXT_TOPPING_MIN_TOUCH = 44.dp

/** 행간 150% (스펙). 글자 크기에 비례하므로 크기마다 다시 계산 */
private const val TEXT_TOPPING_LINE_HEIGHT_RATIO = 1.5f

/**
 * 글자 크기에 맞춰 행간을 건다. 자간·글꼴은 호출부가 스크립트별로 base 스타일에 미리 박아 둔다
 * (자간은 em 이라 크기에 자동 비례). 측정(자동 크기)과 렌더가 같은 base·식을 써야 높이 계산이 맞다.
 */
private fun TextStyle.withTextToppingSizing(fontSizeSp: Float): TextStyle = copy(
    fontSize = fontSizeSp.sp,
    lineHeight = (fontSizeSp * TEXT_TOPPING_LINE_HEIGHT_RATIO).sp,
)

/**
 * C-106 텍스트 입력 ↔ 배치. 인스타그램 스토리 텍스트처럼 화면 전환이 없다 — 편집 모드(가운데
 * 포커스, 전체 화면 딤, '배치' 제목+✓ 헤더, 하단 색·타입·정렬 툴바)와 배치 모드(저장된 자리, 딤
 * 해제, 팬·핀치, '배치' 제목+X 헤더, 하단 '캔버스에 쌓기' 버튼)를 **같은 화면 안에서** [isEditing]
 * 하나로 가른다. 딤은 다른 캔버스 화면과 같은 [ToppingArrangeCanvasDim]. 글자 크기는 자동(연속
 * 축소), 글꼴·자간은 입력 언어로 가른다([detectTextToppingScript]).
 *
 * ⚠️ [isEditing] 은 `WindowInsets.isImeVisible` 이 아니라 **텍스트 필드의 포커스**로 구동한다
 * ([onFocusChanged]). 시스템 키보드 노출 여부로 이 모드를 걸면 [BasicTextField] 자체가 "키보드가
 * 떠야 보이는" 조건부 노드가 돼서 — 포커스를 받을 대상이 애초에 없어 자판이 영영 못 뜨는 닭과
 * 달걀 문제가 생긴다. 필드는 늘 떠 있고(readOnly 도 안 쓴다), 포커스를 얻으면 편집·잃으면 배치다.
 * 그래서 어떤 경로로 키보드가 내려가든(완료·빈 곳 탭·필드 blur) 툴바·색상바·딤이 함께 사라진다.
 *
 * ⚠️ 배경(캔버스 프레임)은 `imePadding` 을 받지 않는 **정적 레이어**다 — 키보드가 떠도 C-001 과
 * 같은 크기 그대로 있고, 키보드 위에 떠야 하는 것(툴바·색상바)만 아래쪽 [Column] 이 `imePadding`
 * 을 받는다. 편집 중 텍스트만 키보드 위 영역 가운데로 [animatePanelFocusFraction] 으로 보간한다.
 */
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
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var isEditing by remember { mutableStateOf(true) }
    // attach 때 들어오는 최초 false 로 모드가 깜빡이지 않게, 한 번이라도 포커스를 얻은 뒤의 상실만 배치로 친다
    var everFocused by remember { mutableStateOf(false) }

    // 편집 중 텍스트를 "키보드 위 영역" 가운데에 두려면 화면 전체 높이와 캔버스의 화면상 위치가 필요하다
    var rootHeightPx by remember { mutableIntStateOf(0) }
    var canvasTopInRootPx by remember { mutableFloatStateOf(0f) }
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isImeVisible = imeBottomPx > 0

    // C-106 진입과 동시에 자판이 뜬다(스펙 "캔버스 중앙에 생성, 키보드 ↑")
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // 편집 종료의 단일 규칙: 빈 내용이면 생성 취소(C-001 복귀), 내용 있으면 포커스만 내려 배치로.
    // ✓·빈 곳 탭·안드로이드 백 세 경로가 모두 이걸 부른다 — placeholder 만 있는 채로 배치에 남지 않게
    val endEditing: () -> Unit = {
        if (uiState.content.isBlank()) {
            onClickConfirm()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // 안드로이드 백 버튼은 시스템이 IME 만 내리고 필드 포커스는 그대로 둔다(= isEditing 유지) — 그
    // 첫 백은 BackHandler 로도 못 잡는다. 그래서 "포커스가 살아 있는데 키보드만 사라진" 순간을
    // 집어 종료로 친다. 포커스를 잃고 내려간 경우(다이얼로그 등)는 onFocusChanged 가 먼저 isEditing 을
    // 내려서 여기 조건(isEditing)에 안 걸리므로, 이 관측은 사실상 '백으로 인한 IME dismiss'만 집는다.
    var keyboardWasShown by remember { mutableStateOf(false) }
    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            keyboardWasShown = true
        } else if (keyboardWasShown && isEditing) {
            endEditing()
        }
    }

    // 키보드가 떠 있는 동안의 백이 앱까지 닿는 드문 경우(일부 기기·외장 키보드)를 위한 명시적 경로.
    // 보통은 IME 가 백을 먼저 먹어 위 관측이 처리한다 — 둘은 서로 다른 경우를 덮는다
    BackHandler(enabled = isEditing) { endEditing() }

    val resumeEditing: () -> Unit = {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // 자동 크기: Text-Area(캔버스 상단~툴바 위, = 캔버스높이 − 키보드 − 툴바52) 안에 모든 줄이
    // 들어오는 가장 큰 크기를 24sp 부터 연속 축소로 찾는다. 가로는 글자 최대 335px(캔버스 폭 제한).
    // 편집 중에만 다시 재고, 배치 모드에선 마지막 값을 얼려 둔다(핀치 배율은 이 위에 별도 적용).
    val textMeasurer = rememberTextMeasurer()
    // 입력 언어로 글꼴·자간을 통째 고른다(일→Line Seed JP, 중→TW/자간0%, 그 외→SUIT/자간-1.1%).
    // base 에 박아 두면 아래 측정·렌더가 같은 글꼴로 돌아 자동 크기 높이 계산이 어긋나지 않는다
    val textScript = remember(uiState.content) { detectTextToppingScript(uiState.content) }
    val autoSizeBaseStyle = YGTheme.typography.body.b01R.copy(
        fontFamily = textScript.fontFamily,
        letterSpacing = textScript.letterSpacingEm.em,
    )
    // 글자 래핑 폭(글자 최대 335px, 캔버스 폭 제한). 이 값으로 측정도 하고 실제 필드 폭도 묶어야
    // 화면 래핑과 측정이 일치하고 335px 를 넘지 않는다
    val textMaxWidthDp = uiState.canvasSize?.width?.coerceAtMost(TEXT_TOPPING_MAX_TEXT_WIDTH)
        ?: TEXT_TOPPING_MAX_TEXT_WIDTH
    val textMaxWidthPx = with(density) { textMaxWidthDp.toPx().toInt() }
    val textAreaHeightPx = uiState.canvasSize?.let { canvasSize ->
        with(density) {
            val canvasHeightPx = canvasSize.height.toPx()
            val toolbarTopPx = rootHeightPx - imeBottomPx - TEXT_TOPPING_TOOLBAR_HEIGHT.toPx()
            (toolbarTopPx - canvasTopInRootPx).coerceIn(0f, canvasHeightPx).toInt()
        }
    } ?: 0
    var autoFontSizeSp by remember { mutableFloatStateOf(TEXT_TOPPING_FONT_SIZE_SP) }
    LaunchedEffect(uiState.content, textAreaHeightPx, textMaxWidthPx, isEditing) {
        if (!isEditing || textAreaHeightPx <= 0 || textMaxWidthPx <= 0) return@LaunchedEffect
        // 연속 변화(빠른 타이핑·키보드 애니메이션) 중엔 멈춘 뒤에만 다시 잰다 — 매 프레임 측정 방지
        delay(AUTO_SIZE_DEBOUNCE_MS)
        autoFontSizeSp = computeAutoFontSizeSp(
            measurer = textMeasurer,
            text = uiState.content,
            maxWidthPx = textMaxWidthPx,
            maxHeightPx = textAreaHeightPx,
            baseStyle = autoSizeBaseStyle,
            maxSp = TEXT_TOPPING_FONT_SIZE_SP,
        )
    }
    val fontSizeSp by animateFloatAsState(targetValue = autoFontSizeSp, label = "textToppingAutoSize")

    val focusFraction by animatePanelFocusFraction(isEditing)
    val storedCenter = DpOffset(
        x = uiState.offsetX + (uiState.textBoxSize?.width ?: 0.dp) / 2,
        y = uiState.offsetY + (uiState.textBoxSize?.height ?: 0.dp) / 2,
    )
    // 편집 중 중심: Text-Area(캔버스 상단~툴바 위) 한가운데. 사이징에 쓰는 [textAreaHeightPx] 와
    // 똑같은 높이의 절반을 캔버스 로컬 y 로 쓴다 — 그래야 Text-Area 높이까지 커진 텍스트가 [0,
    // textAreaHeight] ⊂ 캔버스 안에 머물고 상단으로 삐져나가지 않는다. 화면 높이를 못 잰 첫
    // 프레임엔 캔버스 정중앙으로 폴백(y≈0 플래시 방지)
    val editingCenter = uiState.canvasSize?.let { canvasSize ->
        if (rootHeightPx <= 0) {
            DpOffset(x = canvasSize.width / 2, y = canvasSize.height / 2)
        } else {
            with(density) {
                DpOffset(x = canvasSize.width / 2, y = (textAreaHeightPx / 2f).toDp())
            }
        }
    } ?: storedCenter
    val drawnCenter = lerp(storedCenter, editingCenter, focusFraction)

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

    // 텍스트는 실루엣이 없어 outline = null 로 넘긴다 — ToppingHitTarget 이 사각형 판정으로 떨어진다.
    // 글자가 작아도 판정 영역은 최소 44×44 로 넓혀 둔다(시각 크기는 그대로)
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
        // 캔버스 프레임 바깥(여백·상태바 자리)까지 같은 톤으로 깐 전체 화면 딤. 다른 캔버스
        // 화면들의 딤([ToppingArrangeCanvasDim] = Black25)과 같은 톤이다. imePadding 을 안 받는다 —
        // 키보드가 그 위를 그대로 덮으므로 밑에서 비워 둘 필요가 없다
        if (isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(YGAtomicColors.Transparency.Black25),
            )
        }

        // 캔버스를 그대로 가져온다 — YGCanvas 와 같은 9:16 비율·좌우 padding7 여백. 키보드와
        // 무관한 정적 레이어라 자판이 떠도 배경이 줄거나 움직이지 않는다
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = YGTheme.layout.padding.padding7),
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(CANVAS_AREA_ASPECT_RATIO)
                    .onGloballyPositioned { coordinates ->
                        canvasTopInRootPx = coordinates.boundsInRoot().top
                    },
            ) {
                ToppingArrangeCanvasSurface(
                    backgroundColor = uiState.backgroundColor,
                    backgroundImageUrl = uiState.backgroundImageUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
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
                    if (isEditing) {
                        ToppingArrangeCanvasDim()
                    }

                    // 편집 중 빈 곳 탭 — 텍스트 **뒤**에 깐다. 텍스트 자체를 누르면 필드(커서)가
                    // 받고, 바깥을 누르면 이 레이어가 받아 키보드를 내린다
                    if (isEditing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickableYGNoRipple {
                                    if (uiState.isColorPopoverOpen) onToggleColorPopover() else endEditing()
                                },
                        )
                    }

                    // 텍스트는 늘 같은 자리(centeredAt + graphicsLayer)에 그린다. 끌기 감지는
                    // graphicsLayer 앞에 달아 델타가 캔버스 좌표로 들어오게 한다(편집 중 scale=1).
                    // 모드와 무관하게 늘 달아 둬야 끌기 도중 배치로 바뀌어도 제스처가 안 끊긴다
                    Box(
                        modifier = Modifier
                            .centeredAt(drawnCenter)
                            .onSizeChanged { size ->
                                with(density) {
                                    onTextBoxMeasured(DpSize(size.width.toDp(), size.height.toDp()))
                                }
                            }.textEditDragInput(
                                enabled = { isEditing && uiState.content.isNotBlank() },
                                onDragStart = onEditDragStart,
                                onDrag = onEditDrag,
                            ).graphicsLayer(
                                scaleX = uiState.scale,
                                scaleY = uiState.scale,
                                rotationZ = uiState.rotationDegrees,
                            ),
                    ) {
                        TextToppingBackground(
                            type = uiState.textType,
                            color = uiState.selectedColor,
                            fontSizeSp = fontSizeSp,
                        ) {
                            // 늘 떠 있고 readOnly 도 아니다 — 포커스 유무가 곧 편집/배치다(클래스 KDoc 참고)
                            BasicTextField(
                                value = uiState.content,
                                onValueChange = onContentChange,
                                textStyle = autoSizeBaseStyle
                                    .withTextToppingSizing(fontSizeSp)
                                    .copy(
                                        color = uiState.selectedColor.stroke,
                                        textAlign = uiState.align,
                                    ),
                                modifier = Modifier
                                    // 글자 폭을 래핑 폭(=측정 폭)으로 묶는다 — 화면이 측정보다 넓게
                                    // 흐르거나 335px 를 넘지 않게. 짧은 글자는 그대로 hug 된다
                                    .widthIn(max = textMaxWidthDp)
                                    .focusRequester(focusRequester)
                                    // 포커스 유무만 모드에 반영한다. "빈 내용이면 취소"는 의도한 종료
                                    // 지점(endEditing·백 감지)에서만 판정한다 — 다이얼로그 등이 포커스를
                                    // 잠깐 가져가는 경우까지 생성 취소로 처리하지 않기 위함
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            everFocused = true
                                            isEditing = true
                                        } else if (everFocused) {
                                            isEditing = false
                                        }
                                    },
                                // 빈 상태 플레이스홀더. 선택 색을 흐리게 깐 힌트로, 글자 크기·정렬은 본문과 동일
                                decorationBox = { innerTextField ->
                                    if (uiState.content.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.canvas_text_input_placeholder),
                                            style = autoSizeBaseStyle
                                                .withTextToppingSizing(fontSizeSp)
                                                .copy(
                                                    color = uiState.selectedColor.stroke
                                                        .copy(alpha = TEXT_TOPPING_PLACEHOLDER_ALPHA),
                                                    textAlign = uiState.align,
                                                ),
                                        )
                                    }
                                    innerTextField()
                                },
                            )
                        }
                    }

                    // 배치 모드의 탭·팬·핀치는 텍스트 **위** 전면 오버레이가 독점한다 — 필드가
                    // 포인터를 먼저 가로채면 재탭(onHit→resumeEditing)이 안 뜬다
                    if (!isEditing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .toppingTapInput(
                                    entries = { listOfNotNull(hitTarget()?.let { target -> Unit to target }) },
                                    keyOf = { it },
                                    onHit = { resumeEditing() },
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
        }

        // 헤더 — 정적(상태바 자리). 양쪽 모드 모두 "배치" 제목을 상단 가운데 둔다. 편집은 ✓(편집
        // 마치고 배치로), 배치는 X(그만두기)를 제목과 함께 띄운다
        if (isEditing) {
            YGFloatingBarTitleConfirm(
                title = stringResource(R.string.canvas_text_input_title),
                onConfirmClick = endEditing,
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

        // 편집 모드: 색상바·툴바만 키보드 위에 뜬다. 빈 곳(버튼 사이)을 눌러도 밑의 배경
        // 탭-아웃으로 새지 않도록 이 영역 전체가 터치를 흡수한다
        if (isEditing) {
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
            // 배치 모드: 바닥에 '캔버스에 쌓기' CTA(YGButton Large = 상하 padding5·좌우 padding4)
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

/**
 * [maxSp] 부터 [text] 가 [maxWidthPx]×[maxHeightPx] 안에 모든 줄이 들어오는 가장 큰 크기를 이진
 * 탐색으로 찾는다. 하한은 없지만 0·무한 루프를 막게 [TEXT_TOPPING_MIN_FONT_SIZE_SP] 를 바닥으로 둔다.
 * 텍스트 높이는 크기에 대해 단조라 이진 탐색이 성립한다.
 */
private fun computeAutoFontSizeSp(
    measurer: TextMeasurer,
    text: String,
    maxWidthPx: Int,
    maxHeightPx: Int,
    baseStyle: TextStyle,
    maxSp: Float,
): Float {
    if (text.isEmpty()) return maxSp

    fun fitsAt(sizeSp: Float): Boolean {
        val height = measurer
            .measure(
                text = text,
                style = baseStyle.withTextToppingSizing(sizeSp),
                constraints = Constraints(maxWidth = maxWidthPx),
            ).size.height
        return height <= maxHeightPx
    }

    if (fitsAt(maxSp)) return maxSp

    var low = TEXT_TOPPING_MIN_FONT_SIZE_SP
    var high = maxSp
    repeat(AUTO_SIZE_SEARCH_ITERATIONS) {
        val mid = (low + high) / 2f
        if (fitsAt(mid)) low = mid else high = mid
    }
    return low
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

/** 100자 도달 시 카운터가 빨간색으로 바뀌는지 확인용. */
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
