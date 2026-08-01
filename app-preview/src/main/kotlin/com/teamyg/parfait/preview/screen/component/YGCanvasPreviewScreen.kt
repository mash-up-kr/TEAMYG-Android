package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvas
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuAction
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuItem
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private val PreviewAddAction = YGCanvasMenuAction(
    text = "토핑 추가",
    iconResource = R.drawable.ic_plus,
    onClick = {},
)

private val PreviewEditAction = YGCanvasMenuAction(
    text = "캔버스 편집",
    iconResource = R.drawable.ic_caret_right,
    onClick = {},
)

@Composable
internal fun YGCanvasPreviewScreen(
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
                PreviewSection("Status=Default (Solid 배경)") {
                    YGCanvas(
                        date = "May 20",
                        day = "(Wed)",
                        onDateSelectClick = {},
                        addAction = PreviewAddAction,
                        editAction = PreviewEditAction,
                    )
                }
            }
            item {
                PreviewSection("Status=Empty") {
                    YGCanvas(
                        date = "May 20",
                        day = "(Wed)",
                        onDateSelectClick = {},
                        addAction = PreviewAddAction,
                        editAction = PreviewEditAction,
                        isEmpty = true,
                        emptyMessage = "아직 캔버스가 비어 있어요\n첫번째 토핑을 올려 캔버스를 채워보세요",
                    )
                }
            }
            item {
                PreviewSection("Status=Expanded (dim + 메뉴가 dim 위)") {
                    YGCanvas(
                        date = "May 20",
                        day = "(Wed)",
                        onDateSelectClick = {},
                        addAction = PreviewAddAction,
                        editAction = PreviewEditAction,
                        isDimmed = true,
                        isMenuExpanded = true,
                        expandedItems = listOf(
                            YGCanvasMenuItem(text = "카메라로 촬영", onClick = {}),
                            YGCanvasMenuItem(text = "갤러리에서 선택", onClick = {}),
                        ),
                    )
                }
            }
            item {
                PreviewSection("Status=Spotlighted (dim이 전부 덮음)") {
                    YGCanvas(
                        date = "May 20",
                        day = "(Wed)",
                        onDateSelectClick = {},
                        addAction = PreviewAddAction,
                        editAction = PreviewEditAction,
                        isDimmed = true,
                    )
                }
            }
            item {
                PreviewSection("Status=Calendar (슬롯 placeholder)") {
                    YGCanvas(
                        date = "May 20",
                        day = "(Wed)",
                        onDateSelectClick = {},
                        addAction = PreviewAddAction,
                        editAction = PreviewEditAction,
                        isDimmed = true,
                        isCalendarVisible = true,
                        calendarContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(423.dp)
                                    .background(color = Color.White),
                            )
                        },
                    )
                }
            }
            item {
                PreviewSection("배경 Image(URL)") {
                    YGCanvas(
                        date = "May 20",
                        day = "(Wed)",
                        onDateSelectClick = {},
                        addAction = PreviewAddAction,
                        editAction = PreviewEditAction,
                        background = YGCanvasBackground.Image(
                            url = "https://picsum.photos/seed/parfait/720/1280",
                        ),
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGCanvasPreviewScreen() = PreviewBox {
    YGCanvasPreviewScreen(
        onBack = {},
    )
}
