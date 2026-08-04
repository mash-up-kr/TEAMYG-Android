package com.teamyg.parfait.core.designsystem.component.ygcanvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenu
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuAction
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuItem
import com.teamyg.parfait.core.designsystem.component.ygcanvasdateselect.YGCanvasDateSelectButton
import com.teamyg.parfait.core.designsystem.shape.canvasCutCornerShape
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private const val CANVAS_AREA_ASPECT_RATIO = 9f / 16f

/**
 * Figma Canvas
 */
@Composable
fun YGCanvas(
    date: String,
    day: String,
    onDateSelectClick: () -> Unit,
    addAction: YGCanvasMenuAction,
    editAction: YGCanvasMenuAction,
    modifier: Modifier = Modifier,
    background: YGCanvasBackground = YGCanvasBackground.Solid(YGAtomicColors.Gray.Gray100),
    isDimmed: Boolean = false,
    onDimClick: () -> Unit = {},
    isMenuExpanded: Boolean = false,
    isEmpty: Boolean = false,
    isCalendarVisible: Boolean = false,
    expandedItems: List<YGCanvasMenuItem> = emptyList(),
    emptyMessage: String = "",
    calendarContent: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {},
) {
    val shape = canvasCutCornerShape()

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(-1.dp),
        ) {
            CanvasArea(
                shape = shape,
                background = background,
                isEmpty = isEmpty,
                emptyMessage = emptyMessage,
                content = content,
                dateSelect = {
                    if (isCalendarVisible.not()) {
                        YGCanvasDateSelectButton(
                            date = date,
                            day = day,
                            onClick = onDateSelectClick,
                        )
                    }
                },
            )
            if (isMenuExpanded) {
                Spacer(modifier = Modifier.height(SizeTokens.Size44.getDp()))
            } else {
                YGCanvasMenu(
                    addAction = addAction,
                    editAction = editAction,
                )
            }
        }

        if (isDimmed) {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(color = YGAtomicColors.Transparency.Black25)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onDimClick,
                    ),
            )
        }

        if (isCalendarVisible) {
            Column(modifier = Modifier.fillMaxWidth()) {
                YGCanvasDateSelectButton(
                    date = date,
                    day = day,
                    onClick = onDateSelectClick,
                )
                calendarContent()
            }
        }

        if (isMenuExpanded) {
            YGCanvasMenu(
                addAction = addAction,
                editAction = editAction,
                isExpanded = true,
                expandedItems = expandedItems,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun CanvasArea(
    shape: Shape,
    background: YGCanvasBackground,
    isEmpty: Boolean,
    emptyMessage: String,
    dateSelect: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CANVAS_AREA_ASPECT_RATIO)
            .clip(shape)
            .border(
                width = 1.dp,
                color = YGAtomicColors.Gray.Gray500,
                shape = shape,
            ),
    ) {
        when (background) {
            is YGCanvasBackground.Solid -> Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color = background.color),
            )

            is YGCanvasBackground.Image -> AsyncImage(
                model = background.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }

        content()

        if (isEmpty) {
            Text(
                text = emptyMessage,
                style = YGTheme.typography.caption.c01M,
                color = YGAtomicColors.Gray.Gray500,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = YGTheme.layout.padding.padding6),
            )
        }

        dateSelect()
    }
}

@YGPreview
@Composable
private fun YGCanvasPreview() = PreviewBox {
    YGCanvas(
        date = "May 20",
        day = "(Wed)",
        onDateSelectClick = {},
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
        isEmpty = true,
        emptyMessage = "아직 캔버스가 비어 있어요\n첫번째 토핑을 올려 캔버스를 채워보세요",
    )
}
