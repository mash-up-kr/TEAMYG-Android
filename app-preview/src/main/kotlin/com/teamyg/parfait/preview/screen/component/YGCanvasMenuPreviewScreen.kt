package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenu
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuAction
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuItem
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGCanvasMenuPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        YGTopBarBack(onIconClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                var expanded by remember { mutableStateOf(false) }
                PreviewSection("interactive (tap 캔버스 편집 to toggle expand)") {
                    YGCanvasMenu(
                        addAction = YGCanvasMenuAction(
                            text = "토핑 추가",
                            iconResource = R.drawable.ic_plus,
                            onClick = {},
                        ),
                        editAction = YGCanvasMenuAction(
                            text = "캔버스 편집",
                            iconResource = R.drawable.ic_caret_right,
                            onClick = { expanded = !expanded },
                        ),
                        isExpanded = expanded,
                        expandedItems = listOf(
                            YGCanvasMenuItem(text = "카메라로 촬영", onClick = {}),
                            YGCanvasMenuItem(text = "갤러리에서 선택", onClick = {}),
                        ),
                    )
                }
            }
            item {
                PreviewSection("static: default / expanded") {
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
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGCanvasMenuPreviewScreen() = PreviewBox {
    YGCanvasMenuPreviewScreen(
        onBack = {},
    )
}
