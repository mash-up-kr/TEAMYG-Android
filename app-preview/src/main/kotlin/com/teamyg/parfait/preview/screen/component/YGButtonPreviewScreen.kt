package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGButtonPreviewScreen(
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
                PreviewSection("SmallSquare") {
                    YGButton(
                        text = "버튼",
                        buttonType = YGButtonType.SmallSquare,
                        isEnabled = true,
                        onClick = {},
                    )
                }
            }
            item {
                PreviewSection("Medium.Primary (enabled / disabled)") {
                    YGButton(
                        text = "확인",
                        buttonType = YGButtonType.Medium.Primary,
                        isEnabled = true,
                        onClick = {},
                    )
                    YGButton(
                        text = "확인",
                        buttonType = YGButtonType.Medium.Primary,
                        isEnabled = false,
                        onClick = {},
                    )
                }
            }
            item {
                PreviewSection("Medium.Secondary (enabled / disabled)") {
                    YGButton(
                        text = "취소",
                        buttonType = YGButtonType.Medium.Secondary,
                        isEnabled = true,
                        onClick = {},
                    )
                    YGButton(
                        text = "취소",
                        buttonType = YGButtonType.Medium.Secondary,
                        isEnabled = false,
                        onClick = {},
                    )
                }
            }
            item {
                PreviewSection("Medium.Transparency") {
                    YGButton(
                        text = "투명",
                        buttonType = YGButtonType.Medium.Transparency,
                        isEnabled = true,
                        onClick = {},
                    )
                }
            }
            item {
                PreviewSection("Large (enabled / disabled)") {
                    YGButton(
                        text = "다음",
                        buttonType = YGButtonType.Large,
                        isEnabled = true,
                        onClick = {},
                    )
                    YGButton(
                        text = "다음",
                        buttonType = YGButtonType.Large,
                        isEnabled = false,
                        onClick = {},
                    )
                }
            }
            item {
                PreviewSection("with icons (start / end)") {
                    YGButton(
                        text = "아이콘",
                        buttonType = YGButtonType.Medium.Primary,
                        isEnabled = true,
                        onClick = {},
                        startIconResource = R.drawable.ic_plus,
                    )
                    YGButton(
                        text = "복사",
                        buttonType = YGButtonType.Medium.Secondary,
                        isEnabled = true,
                        onClick = {},
                        endIconResource = R.drawable.ic_copy,
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGButtonPreviewScreen() = PreviewBox {
    YGButtonPreviewScreen(
        onBack = {},
    )
}
