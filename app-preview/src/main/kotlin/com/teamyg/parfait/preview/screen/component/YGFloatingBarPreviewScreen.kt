package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarBackClose
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarClose
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarEdit
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarEditTab
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGFloatingBarPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        YGTopBarBack(onIconClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PreviewSection("Back-Close") {
                    YGFloatingBarBackClose(
                        onBackClick = {},
                        onCloseClick = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                PreviewSection("Close") {
                    YGFloatingBarClose(
                        onCloseClick = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                PreviewSection("Edit") {
                    YGFloatingBarEdit(
                        title = "토핑 편집",
                        onCloseClick = {},
                        onConfirmClick = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                PreviewSection("Edit-Tab") {
                    var selectedIndex by remember { mutableIntStateOf(0) }
                    YGFloatingBarEditTab(
                        tabs = listOf("영역", "테두리"),
                        selectedIndex = selectedIndex,
                        onTabSelect = { selectedIndex = it },
                        onCloseClick = {},
                        onConfirmClick = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGFloatingBarPreviewScreen() = PreviewBox {
    YGFloatingBarPreviewScreen(onBack = {})
}
