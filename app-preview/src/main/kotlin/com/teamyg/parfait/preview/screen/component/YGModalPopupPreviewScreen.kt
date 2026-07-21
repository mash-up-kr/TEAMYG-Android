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
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGModalPopupPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showModal by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        YGTopBarBack(onIconClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                PreviewSection("tap to open modal") {
                    YGButton(
                        text = "모달 열기",
                        buttonType = YGButtonType.Medium.Primary,
                        isEnabled = true,
                        onClick = { showModal = true },
                    )
                }
            }
        }
    }
    if (showModal) {
        YGModalPopup(
            title = "정말 삭제할까요?",
            body = "삭제하면 되돌릴 수 없어요.",
            iconRes = R.drawable.ic_warning_round,
            secondaryText = "취소",
            onSecondaryClick = { showModal = false },
            primaryText = "삭제",
            onPrimaryClick = { showModal = false },
            onDismissRequest = { showModal = false },
        )
    }
}

@YGPreview
@Composable
private fun PreviewYGModalPopupPreviewScreen() = PreviewBox {
    YGModalPopupPreviewScreen(
        onBack = {},
    )
}
