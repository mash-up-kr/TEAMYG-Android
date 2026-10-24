package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import com.teamyg.parfait.feature.groups.canvas.impl.util.TEXT_TOPPING_MAX_LENGTH
import com.teamyg.parfait.feature.groups.canvas.impl.util.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.util.TextToppingType
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

private val TOOLBAR_BUTTON_SIZE = 36.dp
private val TOOLBAR_BUTTON_PADDING = 6.dp

/** 타입 버튼 "A" 프리뷰 치수 — "A" 글자를 감싸는 칩의 여백·테두리 */
private val TYPE_PREVIEW_PADDING_HORIZONTAL = 6.dp
private val TYPE_PREVIEW_STROKE_WIDTH = 1.dp

/**
 * C-106의 입력 상태 상태바. 자판이 뜬 동안만 보인다.
 */
@Composable
internal fun CanvasTextInputToolbar(
    selectedColor: TextToppingColor,
    textType: TextToppingType,
    align: TextAlign,
    characterCount: Int,
    isAtLimit: Boolean,
    onClickColor: () -> Unit,
    onClickType: () -> Unit,
    onClickAlign: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(
            start = YGTheme.layout.padding.padding7,
            end = YGTheme.layout.padding.padding7,
            top = YGTheme.layout.padding.padding3,
            bottom = YGTheme.layout.padding.padding3,
        ),
    ) {
        // 현재 선택한 색의 대표 칩 — 탭하면 8색 팝오버를 연다
        Box(
            modifier = Modifier
                .size(TOOLBAR_BUTTON_SIZE)
                .clip(CircleShape)
                .background(selectedColor.stroke)
                .border(width = 1.dp, color = YGAtomicColors.Transparency.Black5, shape = CircleShape)
                .clickableYGNoRipple(onClick = onClickColor),
        )

        Spacer(modifier = Modifier.width(YGTheme.layout.gap.gap2))

        // 타입(기본/배경/배경+테두리) 순환.
        TypePreviewButton(
            textType = textType,
            onClick = onClickType,
        )

        Spacer(modifier = Modifier.width(YGTheme.layout.gap.gap2))

        // 정렬(가운데/왼쪽/오른쪽) 순환.
        Box(
            modifier = Modifier
                .size(TOOLBAR_BUTTON_SIZE)
                .clip(CircleShape)
                .clickableYGNoRipple(onClick = onClickAlign)
                .padding(TOOLBAR_BUTTON_PADDING),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(align.toAlignIconRes()),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = YGAtomicColors.Transparency.Black75),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "$characterCount/$TEXT_TOPPING_MAX_LENGTH",
            color = if (isAtLimit) YGAtomicColors.Cherry.Cherry500 else YGAtomicColors.Gray.Gray500,
        )
    }
}

/**
 * 현재 타입을 미리 보여 주는 버튼. fill/stroke/radius 세 타입
 */
@Composable
private fun TypePreviewButton(
    textType: TextToppingType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val small = YGTheme.shapes.radius.small

    val letterColor = when (textType) {
        TextToppingType.DEFAULT, TextToppingType.FILL -> YGAtomicColors.Transparency.Black75
        TextToppingType.FILL_STROKE -> YGAtomicColors.Gray.White
    }

    Box(
        modifier = modifier
            .size(TOOLBAR_BUTTON_SIZE)
            .clickableYGNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.then(
                when (textType) {
                    TextToppingType.DEFAULT -> Modifier

                    // 배경O 테두리X
                    TextToppingType.FILL ->
                        Modifier
                            .background(color = YGAtomicColors.Gray.White, shape = small)
                            .padding(
                                horizontal = TYPE_PREVIEW_PADDING_HORIZONTAL,
                                vertical = YGTheme.layout.padding.padding1,
                            )

                    // 배경O 테두리O
                    TextToppingType.FILL_STROKE ->
                        Modifier
                            .border(width = TYPE_PREVIEW_STROKE_WIDTH, color = YGAtomicColors.Gray.White, shape = small)
                            .background(color = YGAtomicColors.Transparency.Black75, shape = small)
                            .padding(
                                horizontal = TYPE_PREVIEW_PADDING_HORIZONTAL,
                                vertical = YGTheme.layout.padding.padding1,
                            )
                },
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "A",
                style = YGTheme.typography.body.b01B,
                color = letterColor,
            )
        }
    }
}

private fun TextAlign.toAlignIconRes(): Int = when (this) {
    TextAlign.Left -> DesignSystemR.drawable.ic_align_left
    TextAlign.Right -> DesignSystemR.drawable.ic_align_right
    else -> DesignSystemR.drawable.ic_align_center
}

@YGPreview
@Composable
private fun CanvasTextInputToolbarPreview() = PreviewBox {
    CanvasTextInputToolbar(
        selectedColor = TextToppingColor.Pink,
        textType = TextToppingType.FILL,
        align = TextAlign.Left,
        characterCount = 12,
        isAtLimit = false,
        onClickColor = {},
        onClickType = {},
        onClickAlign = {},
        modifier = Modifier.background(YGAtomicColors.Transparency.White75),
    )
}

/** 100자 도달 시 카운터가 빨간색이 되는지 확인용. */
@YGPreview
@Composable
private fun CanvasTextInputToolbarAtLimitPreview() = PreviewBox {
    CanvasTextInputToolbar(
        selectedColor = TextToppingColor.Black,
        textType = TextToppingType.DEFAULT,
        align = TextAlign.Center,
        characterCount = TEXT_TOPPING_MAX_LENGTH,
        isAtLimit = true,
        onClickColor = {},
        onClickType = {},
        onClickAlign = {},
        modifier = Modifier.background(YGAtomicColors.Transparency.White75),
    )
}
