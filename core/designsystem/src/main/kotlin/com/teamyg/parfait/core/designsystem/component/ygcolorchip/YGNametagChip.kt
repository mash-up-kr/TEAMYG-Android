package com.teamyg.parfait.core.designsystem.component.ygcolorchip

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

sealed interface YGNametagChipStyle {
    val colorChipSize: Dp
    val colorChipWidth: Dp
    val textStyle: TextStyle
        @Composable get

    data object Style28 : YGNametagChipStyle {
        override val colorChipSize = 28.dp
        override val colorChipWidth = 0.75.dp
        override val textStyle: TextStyle
            @Composable get() = YGTheme.typography.caption.c01R
    }

    data object Style40 : YGNametagChipStyle {
        override val colorChipSize = 40.dp
        override val colorChipWidth = 1.dp
        override val textStyle: TextStyle
            @Composable get() = YGTheme.typography.body.b01R
    }
}

@Composable
fun YGNametagChip(
    colorChipType: YGColorChipType,
    userFirstName: String,
    chip: YGNametagChipStyle,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(chip.colorChipSize)
            .clip(CircleShape)
            .background(colorChipType.fillColor)
            .border(
                width = chip.colorChipWidth,
                color = colorChipType.strokeColor,
                shape = CircleShape,
            ),
    ) {
        // 줄 상자 기준 가운데 정렬은 g·y·p 같은 소문자를 아래로 처지게 한다. 줄 높이는 모든 글자를
        // 같은 양만큼 옮길 뿐이라, 글리프 잉크 경계의 세로 중심을 칩 중심에 맞춘다.
        // TextLayoutResult.getPathForRange 는 Android 에서 선택 영역 사각형을 돌려줘 잉크 경계로 못 쓴다.
        val textStyle = chip.textStyle
        val fontFamilyResolver = LocalFontFamilyResolver.current
        val density = LocalDensity.current
        val inkPaint = remember(textStyle, fontFamilyResolver, density) {
            Paint().apply {
                typeface = fontFamilyResolver
                    .resolve(
                        fontFamily = textStyle.fontFamily,
                        fontWeight = textStyle.fontWeight ?: FontWeight.Normal,
                        fontStyle = textStyle.fontStyle ?: FontStyle.Normal,
                        fontSynthesis = textStyle.fontSynthesis ?: FontSynthesis.All,
                    ).value as Typeface
                textSize = with(density) { textStyle.fontSize.toPx() }
            }
        }
        var inkOffsetY by remember { mutableFloatStateOf(0f) }
        Text(
            text = userFirstName,
            color = colorChipType.textColor,
            style = textStyle,
            onTextLayout = { layout ->
                val ink = Rect()
                inkPaint.getTextBounds(userFirstName, 0, userFirstName.length, ink)
                inkOffsetY = if (ink.isEmpty) {
                    0f
                } else {
                    layout.size.height / 2f - (layout.firstBaseline + ink.exactCenterY())
                }
            },
            modifier = Modifier.graphicsLayer { translationY = inkOffsetY },
        )
    }
}

@YGPreview
@Composable
private fun YGNametagChipPreview(
    @PreviewParameter(YGNametagChipPreviewParameterProvider::class)
    data: YGChipPreviewData,
) = PreviewBox {
    Column {
        Text(data.name)
        Spacer(modifier = Modifier.height(5.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            YGNametagChip(
                colorChipType = data.colorChipType,
                userFirstName = "문",
                chip = YGNametagChipStyle.Style28,
            )
            YGNametagChip(
                colorChipType = data.colorChipType,
                userFirstName = "문",
                chip = YGNametagChipStyle.Style40,
            )
        }
    }
}
