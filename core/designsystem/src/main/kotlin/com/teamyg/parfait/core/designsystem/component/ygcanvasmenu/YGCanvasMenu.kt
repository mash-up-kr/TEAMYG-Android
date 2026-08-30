package com.teamyg.parfait.core.designsystem.component.ygcanvasmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygmenuitem.YGMenuItem
import com.teamyg.parfait.core.designsystem.component.ygstrokebutton.YGStrokeButton
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Canvas-Menu
 *
 * @param addAction 없으면(`null`) [editAction] 이 [expandedItems] 와 같은 단일 줄
 *  [YGMenuItem] 로 전체 너비를 혼자 차지한다
 */
@Composable
fun YGCanvasMenu(
    addAction: YGCanvasMenuAction?,
    editAction: YGCanvasMenuAction,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    expandedItems: List<YGCanvasMenuItem> = emptyList(),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(-1.dp),
    ) {
        if (isExpanded) {
            expandedItems.forEach { item ->
                YGMenuItem(
                    text = item.text,
                    onClick = item.onClick,
                )
            }
        }
        if (addAction != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(-1.dp),
            ) {
                YGStrokeButton(
                    text = addAction.text,
                    onClick = addAction.onClick,
                    iconResource = addAction.iconResource,
                    isEnabled = addAction.isEnabled,
                    modifier = Modifier.weight(1f),
                )
                YGStrokeButton(
                    text = editAction.text,
                    onClick = editAction.onClick,
                    iconResource = editAction.iconResource,
                    isEnabled = editAction.isEnabled,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            YGMenuItem(
                text = editAction.text,
                onClick = editAction.onClick,
                iconResource = editAction.iconResource,
            )
        }
    }
}

@YGPreview
@Composable
private fun YGCanvasMenuPreview() = PreviewBox {
    Column {
        YGCanvasMenu(
            addAction = YGCanvasMenuAction(
                text = "토핑 추가",
                iconResource = R.drawable.ic_plus,
                onClick = {},
            ),
            editAction = YGCanvasMenuAction(
                text = "캔버스 편집",
                iconResource = R.drawable.ic_caret_right,
                onClick = {},
            ),
        )
        YGCanvasMenu(
            addAction = YGCanvasMenuAction(
                text = "토핑 추가",
                iconResource = R.drawable.ic_plus,
                onClick = {},
            ),
            editAction = YGCanvasMenuAction(
                text = "캔버스 편집",
                iconResource = R.drawable.ic_caret_right,
                onClick = {},
            ),
            isExpanded = true,
            expandedItems = listOf(
                YGCanvasMenuItem(text = "카메라로 촬영", onClick = {}),
                YGCanvasMenuItem(text = "갤러리에서 선택", onClick = {}),
            ),
        )
        YGCanvasMenu(
            addAction = YGCanvasMenuAction(
                text = "토핑 추가",
                iconResource = R.drawable.ic_plus,
                onClick = {},
                isEnabled = false,
            ),
            editAction = YGCanvasMenuAction(
                text = "캔버스 편집",
                iconResource = R.drawable.ic_caret_right,
                onClick = {},
            ),
        )
    }
}
