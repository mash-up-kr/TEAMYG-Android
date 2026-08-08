package com.teamyg.parfait.feature.segmentation.impl.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.editor.DEFAULT_TOPPING_BORDER_COLOR
import com.teamyg.parfait.feature.segmentation.impl.editor.TOPPING_BORDER_COLORS

private val CHIP_SIZE = 36.dp
private val CHIP_BORDER_WIDTH = 1.dp

/**
 * 테두리 색을 고르는 가로 목록. 고르면 그 색으로 테두리가 한 겹 더 얹히므로,
 * 켜진 칩은 가장 바깥 겹의 색을 가리킨다. 투명 칩은 색이 아니라 아무 겹도 두르지 않은 상태다.
 *
 * 좌우 여백은 [modifier] 가 아니라 [contentPadding] 으로 준다.
 * 바깥에서 padding 을 걸면 스크롤 영역까지 좁아져 칩이 화면 끝에 닿기 전에 잘린다.
 */
@Composable
internal fun BorderColorChipRow(
    selectedColor: Color,
    onSelectColor: (Color) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    colors: List<Color> = TOPPING_BORDER_COLORS,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
    ) {
        items(items = colors) { color ->
            // 투명은 칠할 색이 없어 칩을 채우는 대신 사선으로 표시한다
            if (color == Color.Transparent) {
                TransparentBorderColorChip(
                    isSelected = color == selectedColor,
                    onClick = { onSelectColor(color) },
                )
            } else {
                BorderColorChip(
                    color = color,
                    isSelected = color == selectedColor,
                    onClick = { onSelectColor(color) },
                )
            }
        }
    }
}

@Composable
private fun BorderColorChip(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(CHIP_SIZE)
            .clip(CircleShape)
            .background(color)
            .border(width = CHIP_BORDER_WIDTH, color = YGAtomicColors.Transparency.Black5, shape = CircleShape)
            .clickable(onClick = onClick),
    ) {
        // 흰색부터 검정까지 색이 갈리는 목록이라, 칩 색을 바꾸는 대신 위에 얹어야 어느 색에서나 표시가 남는다
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(YGAtomicColors.Transparency.Black25),
            )
        }
    }
}

/**
 * 색을 얹지 않는 칩. 채울 색이 없어 테두리와 사선만으로 그린다.
 *
 * 덮을 바탕이 없어 [BorderColorChip] 처럼 반투명 칩을 씌우지 못하므로,
 * 예외적으로 테두리와 사선 색 자체를 바꿔 선택을 표시한다.
 */
@Composable
private fun TransparentBorderColorChip(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (isSelected) YGAtomicColors.Gray.Gray850 else YGAtomicColors.Transparency.Black5

    Canvas(
        modifier = modifier
            .size(CHIP_SIZE)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        val strokeWidth = CHIP_BORDER_WIDTH.toPx()

        // 선이 원 밖으로 삐져나가 clip 에 잘리지 않도록 굵기의 절반만큼 안으로 들인다
        drawCircle(
            color = contentColor,
            radius = (size.minDimension - strokeWidth) / 2f,
            style = Stroke(width = strokeWidth),
        )

        // 모서리를 잇는 대각선이라 원에 잘려 지름만큼만 남는다
        drawLine(
            color = contentColor,
            start = Offset(size.width, 0f),
            end = Offset(0f, size.height),
            strokeWidth = strokeWidth,
        )
    }
}

private class BorderColorChipRowPreviewParameterProvider : PreviewParameterProvider<Color> {
    override val values: Sequence<Color> = sequenceOf(DEFAULT_TOPPING_BORDER_COLOR, TOPPING_BORDER_COLORS.last())
}

@YGPreview
@Composable
private fun BorderColorChipRowPreview(
    @PreviewParameter(BorderColorChipRowPreviewParameterProvider::class) selectedColor: Color,
) = PreviewBox {
    BorderColorChipRow(
        selectedColor = selectedColor,
        onSelectColor = {},
    )
}
