package com.teamyg.parfait.feature.segmentation.impl.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import com.teamyg.parfait.feature.segmentation.impl.editor.DEFAULT_TOPPING_BORDER_COLOR
import com.teamyg.parfait.feature.segmentation.impl.editor.TOPPING_BORDER_COLORS
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

private val CHIP_SIZE = 36.dp
private val CHIP_BORDER_WIDTH = 1.dp
private val CHECK_ICON_SIZE = 24.dp

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
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
    ) {
        items(items = colors) { color ->
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
    // 흰색부터 검정까지 색이 갈리는 목록이라, 얹은 체크가 어느 색 위에서나 읽히도록 반투명 검정을 섞어 깔아둔다.
    // 반투명 칩을 한 겹 덮는 것과 같은 색이면서 그릴 것은 배경 하나로 끝난다
    val chipColor = if (isSelected) {
        YGAtomicColors.Transparency.Black25.compositeOver(color)
    } else {
        color
    }

    Box(
        modifier = modifier
            .size(CHIP_SIZE)
            .clip(CircleShape)
            .background(chipColor)
            .border(width = CHIP_BORDER_WIDTH, color = YGAtomicColors.Transparency.Black5, shape = CircleShape)
            .clickableYGNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Image(
                painter = painterResource(DesignSystemR.drawable.ic_check),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = YGAtomicColors.Gray.White),
                modifier = Modifier.size(CHECK_ICON_SIZE),
            )
        }
    }
}

/**
 * 색을 얹지 않는 칩. 채울 색이 없어 테두리와 사선만으로 그린다.
 *
 * 섞을 바탕이 없어 [BorderColorChip] 처럼 색을 어둡게 만들지 못하므로,
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
            .clickableYGNoRipple(onClick = onClick),
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
