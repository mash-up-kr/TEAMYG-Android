package com.teamyg.parfait.core.designsystem.component.ygcanvasdateselect

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.shape.canvasCutCornerShape
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple

/** 이 화면에서만 쓰는 크기라 공용 [YGIconButtonSize] 에는 넣지 않는다 — 터치 영역 없이 그림 크기 그대로다 */
private val CalendarIconSize = 24.dp

/**
 * Figma Canvas/Button-Date-Select
 *
 * @param isSaveVisible 저장 아이콘을 보일지. 저장할 배경·토핑이 없는 빈 캔버스에서는 감춘다
 */
@Composable
fun YGCanvasDateSelectButton(
    date: String,
    day: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSaveVisible: Boolean = false,
    onClickSave: () -> Unit = {},
    saveContentDescription: String? = null,
) {
    val shape = canvasCutCornerShape()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SizeTokens.Size44.getDp())
            .background(
                color = YGAtomicColors.Transparency.White75,
                shape = shape,
            ).clip(shape)
            .border(
                width = 1.dp,
                color = YGAtomicColors.Gray.Gray500,
                shape = shape,
            )
            // 저장 아이콘이 없을 때는 그 자리를 대신할 오른쪽 여백이 없어, 텍스트가 잘린 모서리에
            // 바짝 붙지 않도록 직접 채운다
            .let { if (isSaveVisible) it else it.padding(end = YGTheme.layout.padding.padding6) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = YGTheme.layout.padding.padding6),
        ) {
            CalendarIconButton(onClick = onClick)
            Row(
                horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = YGTheme.layout.gap.gap2),
            ) {
                Text(
                    text = date,
                    style = YGTheme.typography.body.b02R,
                    color = YGAtomicColors.Gray.Gray800,
                )
                Text(
                    text = day,
                    style = YGTheme.typography.body.b02R,
                    color = YGAtomicColors.Gray.Gray300,
                )
            }
        }
        if (isSaveVisible) {
            YGIconButton(
                iconResource = R.drawable.ic_save,
                size = YGIconButtonSize.SIZE_44,
                contentDescription = saveContentDescription,
                onClick = onClickSave,
            )
        }
    }
}

/**
 * [YGIconButton] 을 그대로 쓰지 않는 이유: 그 컴포넌트는 그림보다 넓은 터치 영역을 항상
 * 따로 두는데, 이 버튼은 그림 크기(24dp) 자체가 곧 자리다. 이 화면에서만 한 번 쓰이므로,
 * [YGIconButton] 의 눌림 색상 로직만 그대로 옮겨와 여기 국소적으로 둔다.
 */
@Composable
private fun CalendarIconButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Image(
        painter = painterResource(R.drawable.ic_calender),
        colorFilter = ColorFilter.tint(
            color = if (isPressed) YGAtomicColors.Gray.Gray400 else YGAtomicColors.Gray.Gray300,
        ),
        contentDescription = null,
        modifier = Modifier
            .size(CalendarIconSize)
            .clickableYGNoRipple(
                onClick = onClick,
                interactionSource = interactionSource,
            ),
    )
}

@YGPreview
@Composable
private fun YGCanvasDateSelectButtonPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4)) {
        YGCanvasDateSelectButton(
            date = "May 20",
            day = "(Wed)",
            onClick = {},
        )
        YGCanvasDateSelectButton(
            date = "May 20",
            day = "(Wed)",
            onClick = {},
            isSaveVisible = true,
            onClickSave = {},
        )
    }
}
