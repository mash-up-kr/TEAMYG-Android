package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.card.YGInviteCard
import com.teamyg.parfait.core.designsystem.component.card.YGInviteCardStatus
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGInviteCardPreviewScreen(
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
                PreviewSection("Active") {
                    YGInviteCard(
                        label = "초대 코드",
                        inviteCode = "ABC123",
                        subText = "유효한 코드입니다",
                        status = YGInviteCardStatus.Active,
                        copyButtonText = "복사",
                        onCopyClick = {},
                    )
                }
            }
            item {
                PreviewSection("Invalid") {
                    YGInviteCard(
                        label = "초대 코드",
                        inviteCode = "------",
                        subText = "만료된 코드입니다",
                        status = YGInviteCardStatus.Invalid,
                        copyButtonText = "복사",
                        onCopyClick = {},
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGInviteCardPreviewScreen() = PreviewBox {
    YGInviteCardPreviewScreen(
        onBack = {},
    )
}
