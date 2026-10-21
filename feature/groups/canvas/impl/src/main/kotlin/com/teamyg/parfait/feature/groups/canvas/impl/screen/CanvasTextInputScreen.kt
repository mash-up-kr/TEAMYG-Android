package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
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
import com.teamyg.parfait.core.util.jvm.extension.graphemeClusterCount
import com.teamyg.parfait.core.util.jvm.extension.lastGraphemeCluster
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
import kotlin.math.round
import kotlin.math.roundToInt

/** 글자 가로 최대(글자만 기준, 배경 여백 불포함) */
private val TEXT_TOPPING_MAX_TEXT_WIDTH = 335.dp

/** 편집 중 pin 전, 박스의 아래변을 캔버스의 이 비율 지점에 고정한다(0.5=정중앙 높이) */
private const val TEXT_TOPPING_EDIT_BOTTOM_ANCHOR_RATIO = 0.5f

/** 글자가 아주 작아져도 배치 모드 재탭·드래그 판정 영역은 이보다 작아지지 않는다(시각 크기는 그대로) */
private val TEXT_TOPPING_MIN_TOUCH = 44.dp

/** 행간 150% (스펙). 글자 크기에 비례하므로 크기마다 다시 계산 */
private const val TEXT_TOPPING_LINE_HEIGHT_RATIO = 1.5f

/**
 * 글자 크기에 맞춰 행간을 건다. 자간·글꼴은 호출부가 스크립트별로 base 스타일에 미리 박아 둔다
 * (자간은 em 이라 크기에 자동 비례). hug 폭 측정과 렌더가 같은 base·식을 써야 폭이 맞다.
 */
private fun TextStyle.withTextToppingSizing(fontSizeSp: Float): TextStyle = copy(
    fontSize = fontSizeSp.sp,
    lineHeight = (fontSizeSp * TEXT_TOPPING_LINE_HEIGHT_RATIO).sp,
)

/**
 * 편집↔배치 전환용 세로 배치. 배치([focusFraction]=0)는 [arrangeCenter] 에 박스 중심을 맞춘다
 * (기존 동작). 편집([focusFraction]=1)은 박스를 가로로 캔버스 중앙에 두고 윗변을 [editTopPx] 에 맞춘다
 */
private fun Modifier.editingTopAnchored(
    arrangeCenter: DpOffset,
    editTopPx: Int,
    canvasWidthPx: Int,
    focusFraction: Float,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        val arrangeLeft = arrangeCenter.x.roundToPx() - placeable.width / 2
        val arrangeTop = arrangeCenter.y.roundToPx() - placeable.height / 2
        val editLeft = (canvasWidthPx - placeable.width) / 2
        placeable.placeRelative(
            x = arrangeLeft + ((editLeft - arrangeLeft) * focusFraction).roundToInt(),
            y = arrangeTop + ((editTopPx - arrangeTop) * focusFraction).roundToInt(),
        )
    }
}

/**
 * 박스가 [maxWidthPx]×[maxHeightPx] 를 넘으면 **전체가 다 들어오게 비율 축소**한다.
 * 편집 중엔 [maxWidthPx] 를 무한대로 넘겨 높이만 제한하고, 배치 모드에선 캔버스
 * [onNaturalSizeChanged] 는 **스케일 전** 원본 크기를 보고한다.
 */
private fun Modifier.scaleBoxToFit(
    maxWidthPx: Int,
    maxHeightPx: Int,
    onNaturalSizeChanged: (IntSize) -> Unit = {},
): Modifier = layout { measurable, constraints ->
    // 가로·세로 제한을 풀고 재야 박스의 **자연 크기**(안 잘린 전체 크기)가 나온다.
    val placeable = measurable.measure(Constraints())
    onNaturalSizeChanged(IntSize(placeable.width, placeable.height))
    // 둘 다 안에 들어오면(축소 불필요) **레이어 없이** 그대로 둔다
    // 하나라도 넘치면, 전체가 다 들어오도록 더 작은 쪽 비율로 축소한다
    val fits = (maxWidthPx <= 0 || placeable.width <= maxWidthPx) &&
        (maxHeightPx <= 0 || placeable.height <= maxHeightPx)
    if (fits) {
        return@layout layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }
    val widthScale = if (maxWidthPx > 0) maxWidthPx.toFloat() / placeable.width else 1f
    val heightScale = if (maxHeightPx > 0) maxHeightPx.toFloat() / placeable.height else 1f
    val scale = minOf(widthScale, heightScale)
    val scaledWidth = (placeable.width * scale).roundToInt()
    val scaledHeight = (placeable.height * scale).roundToInt()
    layout(scaledWidth, scaledHeight) {
        placeable.placeRelativeWithLayer(
            x = (scaledWidth - placeable.width) / 2,
            y = 0,
        ) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 0f)
        }
    }
}

/**
 * C-106 텍스트 입력 ↔ 배치. 인스타그램 스토리 텍스트처럼 화면 전환이 없다 — 편집 모드(가운데
 * 포커스, 전체 화면 딤, '배치' 제목+✓ 헤더, 하단 색·타입·정렬 툴바)와 배치 모드(저장된 자리, 딤
 * 해제, 팬·핀치, '배치' 제목+X 헤더, 하단 '캔버스에 쌓기' 버튼)를 **같은 화면 안에서** [isEditing]
 * 하나로 가른다. 딤은 다른 캔버스 화면과 같은 [ToppingArrangeCanvasDim]. 글자는 24sp 고정으로 그리고
 * 영역을 넘으면 박스를 비율 축소([scaleBoxToFit]), 글꼴·자간은 입력 언어로 가른다([detectTextToppingScript]).
 *
 * ⚠️ [isEditing] 은 `WindowInsets.isImeVisible` 이 아니라 **텍스트 필드의 포커스**로 구동한다
 * ([onFocusChanged]). 시스템 키보드 노출 여부로 이 모드를 걸면 [BasicTextField] 자체가 "키보드가
 * 떠야 보이는" 조건부 노드가 돼서 — 포커스를 받을 대상이 애초에 없어 자판이 영영 못 뜨는 닭과
 * 달걀 문제가 생긴다. 필드는 늘 떠 있고(readOnly 도 안 쓴다), 포커스를 얻으면 편집·잃으면 배치다.
 * 그래서 어떤 경로로 키보드가 내려가든(완료·빈 곳 탭·필드 blur) 툴바·색상바·딤이 함께 사라진다.
 *
 * ⚠️ 배경(캔버스 프레임)은 `imePadding` 을 받지 않는 **정적 레이어**다 — 키보드가 떠도 C-001 과
 * 같은 크기 그대로 있고, 키보드 위에 떠야 하는 것(툴바·색상바)만 아래쪽 [Column] 이 `imePadding`
 * 을 받는다. 편집 중 텍스트만 키보드 위 영역 안에서 [animatePanelFocusFraction] 으로 보간한다.
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

    // 줄바꿈 하나당 딱 한 번만 지운다. 그 뒤에 같은 글자가 또 오면 의도한 입력으로 보고 통과시킨다
    val dedupeHangulEnterTransformation = remember {
        var correctedThisNewline = false
        InputTransformation {
            val old = originalText.toString()
            val new = asCharSequence().toString()
            if (!new.startsWith(old)) return@InputTransformation
            val added = new.substring(old.length)
            if (added == "\n") {
                // 새 줄바꿈이 커밋됨 — 이 줄에 대한 보정 기회를 새로 연다
                correctedThisNewline = false
                return@InputTransformation
            }
            if (correctedThisNewline) return@InputTransformation
            if (old.isEmpty() || old.last() != '\n') return@InputTransformation
            val beforeNewline = old.dropLast(1).lastGraphemeCluster() ?: return@InputTransformation

            if (beforeNewline != "\n" && added == beforeNewline) {
                delete(old.length, new.length)
                placeCursorAtEnd()
                correctedThisNewline = true
            }
        }
    }

    val maxLengthTransformation = remember {
        InputTransformation {
            if (asCharSequence().toString().graphemeClusterCount() > TEXT_TOPPING_MAX_LENGTH) {
                revertAllChanges()
            }
        }
    }
    val textFieldState = rememberTextFieldState(initialText = uiState.content)
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { content -> onContentChange(content) }
    }

    var isEditing by remember { mutableStateOf(true) }
    // attach 때 들어오는 최초 false 로 모드가 깜빡이지 않게, 한 번이라도 포커스를 얻은 뒤의 상실만 배치로 친다
    var everFocused by remember { mutableStateOf(false) }

    // 키보드가 내려갔는지 감지용(편집 종료 판정)
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isImeVisible = imeBottomPx > 0
    // 화면(root) 전체 높이. 키보드 윗변의 화면 절대 y 좌표 = rootHeightPx - imeBottomPx 를 구할 때 쓴다
    var rootHeightPx by remember { mutableIntStateOf(0) }

    // C-106 진입과 동시에 자판이 뜬다
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

    // 입력 언어로 글꼴·자간을 통째 고른다(일→Line Seed JP, 중→TW/자간0%, 그 외→SUIT/자간-1.1%).
    val textScript = remember(uiState.content) { detectTextToppingScript(uiState.content) }
    val autoSizeBaseStyle = YGTheme.typography.body.b01R.copy(
        fontFamily = textScript.fontFamily,
        letterSpacing = textScript.letterSpacingEm.em,
    )
    // 좌우 여백
    val editSideMargin = YGTheme.layout.padding.padding2
    // 글자 래핑 폭(최대 335dp, 캔버스 폭에서 좌우 여백을 뺀 값으로 제한).
    val textMaxWidthDp = uiState.canvasSize
        ?.let { (it.width - editSideMargin * 2).coerceAtMost(TEXT_TOPPING_MAX_TEXT_WIDTH) }
        ?: TEXT_TOPPING_MAX_TEXT_WIDTH
    // 캔버스가 화면(root) 안에서 시작하는 y — 아래 두 한계를 캔버스 로컬 좌표로 환산할 때 쓴다
    var canvasTopPx by remember { mutableFloatStateOf(0f) }

    val editAreaHeightPx = with(density) { (uiState.canvasSize?.height ?: 0.dp).roundToPx() }

    // 상단 여백(상태바 바로 아래)과 하단 여백(키보드 바로 위, "살짝 위")
    val topPaddingPx = with(density) { 12.dp.roundToPx() }
    val bottomPaddingPx = with(density) { 12.dp.roundToPx() }

    // 상단 핀 위치: 시스템 상태바 바로 아래 지점을 캔버스 local Y 좌표로 환산.
    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    val editTopMarginPx: Int = if (canvasTopPx > 0f) {
        round(statusBarTopPx - canvasTopPx + topPaddingPx).toInt()
    } else {
        with(density) { 16.dp.roundToPx() }
    }

    // 하단 한계 위치: 키보드 바로 위 지점을 캔버스 local Y 좌표로 환산(
    val editBottomLimitPx: Int = if (rootHeightPx > 0 && canvasTopPx > 0f) {
        val keyboardTopPx = rootHeightPx - imeBottomPx
        round(keyboardTopPx - canvasTopPx - bottomPaddingPx).toInt()
    } else {
        editAreaHeightPx - with(density) { 16.dp.roundToPx() }
    }

    // 세로 예산: 상태바 부근 ~ 키보드 바로 위 사이의 최대 가용 영역
    val editBudgetPx: Int = (editBottomLimitPx - editTopMarginPx).coerceAtLeast(1)

    // 매 편집 세션이 항상 가운데에서 시작하도록 강제한다
    var hasMeasuredThisEditSession by remember { mutableStateOf(false) }
    LaunchedEffect(isEditing) {
        if (isEditing) hasMeasuredThisEditSession = false
    }
    val editBoxHeightPx = if (hasMeasuredThisEditSession) {
        with(density) { (uiState.textBoxSize?.height ?: 0.dp).toPx() }
    } else {
        0f
    }
    // 박스의 **아래변**을 캔버스의 [TEXT_TOPPING_EDIT_BOTTOM_ANCHOR_RATIO] 지점에 고정한다
    val editNaturalTopPx: Int = round(
        editAreaHeightPx * TEXT_TOPPING_EDIT_BOTTOM_ANCHOR_RATIO - editBoxHeightPx,
    ).toInt()
    var everPinned by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.textBoxSize, editAreaHeightPx, editTopMarginPx, isEditing, uiState.content.isEmpty()) {
        if (!isEditing || uiState.content.isEmpty()) {
            everPinned = false
        } else if (editAreaHeightPx > 0 && editNaturalTopPx <= editTopMarginPx) {
            everPinned = true
        }
    }
    // 윗변: pin 전엔 아래변 고정 기준(박스 커질수록 위로만), pin 후엔 상단 여백 고정. 애니메이션
    // 없이 즉시 적용 — 첫 노출·자판 전환 때 미끄러지지 않고 바로 자리에 둔다
    val editTopPx = if (everPinned) editTopMarginPx else maxOf(editTopMarginPx, editNaturalTopPx)

    // [스케일 방식] 글자는 늘 24sp 로 그린다. 박스가 세로 예산을 넘으면 graphicsLayer 로 비율 축소
    val fontSizeSp = TEXT_TOPPING_FONT_SIZE_SP

    val placeholderText = stringResource(R.string.canvas_text_input_placeholder)

    val focusFraction by animatePanelFocusFraction(isEditing)
    val storedCenter = DpOffset(
        x = uiState.offsetX + (uiState.textBoxSize?.width ?: 0.dp) / 2,
        y = uiState.offsetY + (uiState.textBoxSize?.height ?: 0.dp) / 2,
    )
    // 블록(박스)은 가로로 늘 캔버스 중앙에 둔다.
    val canvasWidthPx = with(density) { (uiState.canvasSize?.width ?: 0.dp).roundToPx() }

    // 드래그·히트 판정이 쓰는 "현재 그려진 중심". x 는 캔버스 중앙, y 는 윗변 + 박스높이/2
    val editingCenter = uiState.canvasSize?.let { canvasSize ->
        with(density) {
            DpOffset(x = canvasSize.width / 2, y = (editTopPx + editBoxHeightPx / 2f).toDp())
        }
    } ?: storedCenter
    val drawnCenter = lerp(storedCenter, editingCenter, focusFraction)
    // 편집 중엔 처음 작성할 때처럼 배율·회전 없이(1배·0도) 보여야 한다
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
        if (isEditing) {
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
                    .let { if (isEditing) it else it.clipToBounds() },
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
                }

                Box(
                    modifier = Modifier
                        .editingTopAnchored(
                            arrangeCenter = storedCenter,
                            editTopPx = editTopPx,
                            canvasWidthPx = canvasWidthPx,
                            focusFraction = focusFraction,
                        ).textEditDragInput(
                            enabled = { isEditing && uiState.content.isNotBlank() },
                            onDragStart = onEditDragStart,
                            onDrag = onEditDrag,
                        )
                        .scaleBoxToFit(
                            maxWidthPx = if (isEditing) 0 else canvasWidthPx,
                            maxHeightPx = if (isEditing) editBudgetPx else editAreaHeightPx,
                        ) { naturalSize ->
                            with(density) {
                                onTextBoxMeasured(DpSize(naturalSize.width.toDp(), naturalSize.height.toDp()))
                            }
                            hasMeasuredThisEditSession = true
                        }.graphicsLayer(
                            scaleX = drawnScale,
                            scaleY = drawnScale,
                            rotationZ = drawnRotationDegrees,
                        ),
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
                                    color = uiState.selectedColor.stroke,
                                    textAlign = uiState.align,
                                ),
                            modifier = Modifier
                                // 글자 폭을 래핑 폭(=측정 폭)으로 묶는다
                                .widthIn(max = textMaxWidthDp)
                                .focusRequester(focusRequester)
                                // 포커스 유무만 모드에 반영한다. "빈 내용이면 취소"는 의도한 종료
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        everFocused = true
                                        isEditing = true
                                    } else if (everFocused) {
                                        isEditing = false
                                    }
                                },
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

        // 헤더
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

        // 편집 모드: 색상바·툴바만 키보드 위에 뜬다.
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
