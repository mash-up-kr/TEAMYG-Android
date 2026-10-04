package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygslider.YGSlider
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.util.DEFAULT_TOPPING_BORDER_WIDTH_DP
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BORDER_COLORS
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BORDER_WIDTH_RANGE_DP
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

private const val PANEL_TAG = "topping_border_panel"
private const val TOGGLE_TAG = "topping_border_panel_toggle"
private const val SLIDER_TAG = "topping_border_slider"

private val PANEL_STROKE_WIDTH = 0.5.dp
private val TOGGLE_TOUCH_SIZE = 44.dp
private val TOGGLE_ICON_SIZE = 16.dp
private val LABEL_SLIDER_GAP = 4.dp

/**
 * 캔버스 아래쪽에 붙는 테두리 설정 패널. 닫히면 한 줄짜리 바, 열리면 굵기 슬라이더와 색상칩이다.
 *
 * 자기 영역의 포인터는 빈 곳까지 아래 레이어로 내려보내지 않는다. 패널 밑에 깔린 캔버스 입력이
 * 빈 곳 터치를 받으면 패널이 닫히거나 토핑이 움직인다.
 *
 * @param selectedColorArgb `null` 은 테두리 없음
 * @param isEnabled 닫힌 바의 토글만 막는다
 */
@Composable
internal fun ToppingBorderPanel(
    isOpen: Boolean,
    selectedColorArgb: Int?,
    widthDp: Float,
    widthRange: ClosedFloatingPointRange<Float>,
    onClickToggle: () -> Unit,
    onSelectColor: (Int?) -> Unit,
    onChangeWidth: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(PANEL_TAG)
            .pointerInput(Unit) {}
            .background(if (isOpen) YGAtomicColors.Gray.White else YGAtomicColors.Transparency.White75)
            .sideAndBottomStroke(width = PANEL_STROKE_WIDTH, color = YGAtomicColors.Gray.Gray500),
    ) {
        if (isOpen) {
            OpenPanelContent(
                selectedColorArgb = selectedColorArgb,
                widthDp = widthDp,
                widthRange = widthRange,
                onClickToggle = onClickToggle,
                onSelectColor = onSelectColor,
                onChangeWidth = onChangeWidth,
            )
        } else {
            CollapsedPanelBar(
                onClickToggle = onClickToggle,
                isEnabled = isEnabled,
            )
        }
    }
}

@Composable
private fun CollapsedPanelBar(
    onClickToggle: () -> Unit,
    isEnabled: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TOGGLE_TAG)
            .then(
                if (isEnabled) {
                    Modifier.clickableYGNoRipple(role = Role.Button, onClick = onClickToggle)
                } else {
                    Modifier
                },
            ).padding(YGTheme.layout.padding.padding6),
    ) {
        PanelLabel(
            text = stringResource(R.string.canvas_topping_border_panel_collapsed),
            modifier = Modifier.weight(1f),
        )
        ToggleIcon(
            iconResource = DesignSystemR.drawable.ic_caret_top,
            contentDescription = stringResource(R.string.canvas_topping_border_panel_open),
        )
    }
}

@Composable
private fun OpenPanelContent(
    selectedColorArgb: Int?,
    widthDp: Float,
    widthRange: ClosedFloatingPointRange<Float>,
    onClickToggle: () -> Unit,
    onSelectColor: (Int?) -> Unit,
    onChangeWidth: (Float) -> Unit,
) {
    val horizontalPadding = YGTheme.layout.padding.padding6
    val verticalPadding = YGTheme.layout.padding.padding7
    // 터치 영역은 아이콘보다 넓다. 아이콘이 내용 여백에 맞게 놓이도록 넓어진 만큼 바깥으로 뺀다
    val toggleInset = (TOGGLE_TOUCH_SIZE - TOGGLE_ICON_SIZE) / 2

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = verticalPadding),
        ) {
            PanelLabel(
                text = stringResource(R.string.canvas_topping_border_panel_width),
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )
            Spacer(modifier = Modifier.height(LABEL_SLIDER_GAP))
            YGSlider(
                value = widthDp,
                onValueChange = onChangeWidth,
                valueRange = widthRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
                    .testTag(SLIDER_TAG),
            )
            Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap3))
            BorderColorChipRow(
                selectedColorArgb = selectedColorArgb,
                onSelectColor = onSelectColor,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = (verticalPadding - toggleInset).coerceAtLeast(0.dp),
                    end = (horizontalPadding - toggleInset).coerceAtLeast(0.dp),
                ).size(TOGGLE_TOUCH_SIZE)
                .testTag(TOGGLE_TAG)
                .clickableYGNoRipple(role = Role.Button, onClick = onClickToggle),
        ) {
            ToggleIcon(
                iconResource = DesignSystemR.drawable.ic_caret_bottom,
                contentDescription = stringResource(R.string.canvas_topping_border_panel_close),
            )
        }
    }
}

@Composable
private fun PanelLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = YGTheme.typography.caption.c01M,
        color = YGAtomicColors.Gray.Gray700,
        modifier = modifier,
    )
}

@Composable
private fun ToggleIcon(
    iconResource: Int,
    contentDescription: String,
) {
    Image(
        painter = painterResource(iconResource),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(YGAtomicColors.Gray.Gray700),
        modifier = Modifier.size(TOGGLE_ICON_SIZE),
    )
}

/** 좌·우·하에만 선을 긋는다. 위는 캔버스와 맞닿는 쪽이라 비운다 */
private fun Modifier.sideAndBottomStroke(
    width: Dp,
    color: Color,
): Modifier = drawWithContent {
    drawContent()

    val strokeWidth = width.toPx()
    val inset = strokeWidth / 2f
    drawLine(color, Offset(inset, 0f), Offset(inset, size.height), strokeWidth)
    drawLine(color, Offset(size.width - inset, 0f), Offset(size.width - inset, size.height), strokeWidth)
    drawLine(color, Offset(0f, size.height - inset), Offset(size.width, size.height - inset), strokeWidth)
}

private class ToppingBorderPanelPreviewParameterProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(false, true)
}

@YGPreview
@Composable
private fun ToppingBorderPanelPreview(
    @PreviewParameter(ToppingBorderPanelPreviewParameterProvider::class) isOpen: Boolean,
) = PreviewBox {
    ToppingBorderPanel(
        isOpen = isOpen,
        selectedColorArgb = TOPPING_BORDER_COLORS.last().toArgb(),
        widthDp = DEFAULT_TOPPING_BORDER_WIDTH_DP,
        widthRange = TOPPING_BORDER_WIDTH_RANGE_DP,
        onClickToggle = {},
        onSelectColor = {},
        onChangeWidth = {},
        modifier = Modifier.align(Alignment.BottomCenter),
    )
}
