package com.teamyg.parfait.core.designsystem.component.ygfloatingbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.component.ygedittabbutton.YGEditTabButton
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Floating Bar / Status=Back-Close
 */
@Composable
fun YGFloatingBarBackClose(
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGFloatingBarContent(modifier = modifier) {
        YGCircleButton(
            iconResource = R.drawable.ic_caret_left,
            type = YGCircleButtonType.Default,
            contentDescription = "뒤로가기",
            onClick = onBackClick,
        )
        YGFloatingBarCloseButton(onClick = onCloseClick)
    }
}

/**
 * Figma Floating Bar / Status=Close
 */
@Composable
fun YGFloatingBarClose(
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGFloatingBarContent(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
    ) {
        YGFloatingBarCloseButton(onClick = onCloseClick)
    }
}

/**
 * Figma Floating Bar / Status=Edit
 */
@Composable
fun YGFloatingBarEdit(
    title: String,
    onCloseClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGFloatingBarContent(modifier = modifier) {
        YGFloatingBarCloseButton(onClick = onCloseClick)
        Text(
            text = title,
            style = YGTheme.typography.body.b01R,
            color = YGAtomicColors.Gray.Gray800,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        YGFloatingBarConfirmButton(onClick = onConfirmClick)
    }
}

/**
 * Figma Floating Bar / Status=Edit-Tab
 */
@Composable
fun YGFloatingBarEditTab(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    onCloseClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGFloatingBarContent(modifier = modifier) {
        YGFloatingBarCloseButton(onClick = onCloseClick)
        Row(verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, label ->
                YGEditTabButton(
                    text = label,
                    isSelected = index == selectedIndex,
                    onClick = { onTabSelect(index) },
                )
            }
        }
        YGFloatingBarConfirmButton(onClick = onConfirmClick)
    }
}

@Composable
private fun YGFloatingBarContent(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(
            start = YGTheme.layout.padding.padding7,
            top = YGTheme.layout.padding.padding6,
            end = YGTheme.layout.padding.padding7,
        ),
        content = content,
    )
}

@Composable
private fun YGFloatingBarCloseButton(onClick: () -> Unit) {
    YGCircleButton(
        iconResource = R.drawable.ic_close,
        type = YGCircleButtonType.Default,
        contentDescription = "닫기",
        onClick = onClick,
    )
}

@Composable
private fun YGFloatingBarConfirmButton(onClick: () -> Unit) {
    YGCircleButton(
        iconResource = R.drawable.ic_check,
        type = YGCircleButtonType.Default,
        contentDescription = "확인",
        onClick = onClick,
    )
}

@YGPreview
@Composable
private fun YGFloatingBarPreview() = PreviewBox {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White),
    ) {
        YGFloatingBarBackClose(
            onBackClick = {},
            onCloseClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        YGFloatingBarClose(
            onCloseClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        YGFloatingBarEdit(
            title = "토핑 편집",
            onCloseClick = {},
            onConfirmClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        YGFloatingBarEdit(
            title = "아주아주긴제목입니다정말로그렇습니다",
            onCloseClick = {},
            onConfirmClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        YGFloatingBarEditTab(
            tabs = listOf("영역", "테두리"),
            selectedIndex = 0,
            onTabSelect = {},
            onCloseClick = {},
            onConfirmClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
