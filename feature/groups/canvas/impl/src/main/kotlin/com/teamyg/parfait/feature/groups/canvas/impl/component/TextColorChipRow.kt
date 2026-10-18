package com.teamyg.parfait.feature.groups.canvas.impl.component

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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import com.teamyg.parfait.feature.groups.canvas.impl.util.TextToppingColor
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

private val CHIP_SIZE = 36.dp
private val CHIP_BORDER_WIDTH = 1.dp
private val CHECK_ICON_SIZE = 24.dp

/**
 * 텍스트 글자색을 고르는 가로 목록. 칩은 각 색의 Stroke 톤을 보여 준다 — 흰색부터 검정까지 섞여
 * 있어 선택 체크가 어느 색 위에서나 읽히도록 반투명 검정을 섞어 깐다. 좌우 여백은 [modifier] 가
 * 아니라 [contentPadding] 으로 준다.
 */
@Composable
internal fun TextColorChipRow(
    selectedColor: TextToppingColor,
    onSelectColor: (TextToppingColor) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
    ) {
        items(items = TextToppingColor.entries) { color ->
            TextColorChip(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onSelectColor(color) },
            )
        }
    }
}

@Composable
private fun TextColorChip(
    color: TextToppingColor,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipColor = if (isSelected) {
        YGAtomicColors.Transparency.Black25.compositeOver(color.stroke)
    } else {
        color.stroke
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

private class TextColorChipRowPreviewParameterProvider : PreviewParameterProvider<TextToppingColor> {
    override val values: Sequence<TextToppingColor> = sequenceOf(
        TextToppingColor.White,
        TextToppingColor.Violet,
    )
}

@YGPreview
@Composable
private fun TextColorChipRowPreview(
    @PreviewParameter(TextColorChipRowPreviewParameterProvider::class) selectedColor: TextToppingColor,
) = PreviewBox {
    TextColorChipRow(
        selectedColor = selectedColor,
        onSelectColor = {},
    )
}
