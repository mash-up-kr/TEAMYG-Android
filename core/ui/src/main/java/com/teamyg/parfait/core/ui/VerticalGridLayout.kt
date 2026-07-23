package com.teamyg.parfait.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun <T> VerticalGridLayout(
    items: List<T>,
    columnCount: Int,
    verticalPadding: Dp,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (item: T) -> Unit,
) {
    val rowCount = ((items.size - 1) / columnCount) + 1
    Column(modifier = modifier) {
        repeat(rowCount) { columnIndex ->
            if (columnIndex != 0) {
                Spacer(modifier = Modifier.height(verticalPadding))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
            ) {
                repeat(columnCount) { rowIndex ->
                    if (rowIndex != 0) {
                        Spacer(modifier = Modifier.width(horizontalPadding))
                    }
                    val item = items.getOrNull(columnIndex * columnCount + rowIndex)
                    if (item != null) {
                        Box(modifier = Modifier.weight(1f)) {
                            content(item)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun VerticalGridLayoutPreview() {
    Box(modifier = Modifier.fillMaxWidth()) {
        VerticalGridLayout(
            items = listOf(
                "hello",
                "hello",
                "hello world hello world hello world hello world",
                "hello",
                "hello",
                "hello",
            ),
            columnCount = 5,
            verticalPadding = 5.dp,
            horizontalPadding = 10.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val color = Random.nextLong(0xFF000000, 0xFFFFFFFF)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(color = Color(color)),
            ) {
                Text(
                    text = it,
                    modifier = Modifier
                        .fillMaxHeight(),
                )
            }
        }
    }
}
