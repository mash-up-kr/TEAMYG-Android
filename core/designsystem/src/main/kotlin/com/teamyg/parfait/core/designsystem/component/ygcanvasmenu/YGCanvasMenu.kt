package com.teamyg.parfait.core.designsystem.component.ygcanvasmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygmenuitem.YGMenuItem
import com.teamyg.parfait.core.designsystem.component.ygstrokebutton.YGStrokeButton
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Canvas-Menu
 */
@Composable
fun YGCanvasMenu(
    addAction: YGCanvasMenuAction,
    editAction: YGCanvasMenuAction,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    expandedItems: List<YGCanvasMenuItem> = emptyList(),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (isExpanded) {
            expandedItems.forEach { item ->
                YGMenuItem(
                    text = item.text,
                    onClick = item.onClick,
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            YGStrokeButton(
                text = addAction.text,
                onClick = addAction.onClick,
                iconResource = addAction.iconResource,
                modifier = Modifier.weight(1f),
            )
            YGStrokeButton(
                text = editAction.text,
                onClick = editAction.onClick,
                iconResource = editAction.iconResource,
                modifier = Modifier.weight(1f),
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
    }
}
