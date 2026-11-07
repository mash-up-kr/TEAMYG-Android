package com.teamyg.parfait.feature.app.notice.impl.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.core.designsystem.component.ygchipnew.YGChipNew
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.notice.NoticeVO

private val CaretSize = 24.dp

@Composable
internal fun NoticeListItem(
    notice: NoticeVO,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = YGTheme.layout.padding.padding7,
                    vertical = YGTheme.layout.padding.padding5,
                ),
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = notice.title,
                        style = YGTheme.typography.body.b02R,
                        color = YGAtomicColors.Gray.Gray800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    if (notice.isNew) {
                        YGChipNew()
                    }
                }

                Text(
                    text = notice.date,
                    style = YGTheme.typography.caption.c01R,
                    color = YGAtomicColors.Gray.Gray400,
                )
            }

            Image(
                painter = painterResource(DesignSystemR.drawable.ic_caret_right),
                contentDescription = null,
                modifier = Modifier.size(CaretSize),
            )
        }

        HorizontalDivider(color = YGAtomicColors.Gray.Gray100)
    }
}

@YGPreview
@Composable
private fun NoticeListItemPreview() = PreviewBox {
    val notice = NoticeVO(id = 1L, title = "파르페 v1.5.0 업데이트 안내", date = "2026.11.12", content = "", isNew = false)

    Column {
        NoticeListItem(notice = notice.copy(isNew = true), onClick = {})
        NoticeListItem(notice = notice, onClick = {})
        NoticeListItem(
            notice = notice.copy(
                title = "이용약관 및 개인정보처리방침 개정 안내가 아주 길어서 한 줄을 넘어가는 경우의 공지",
                isNew = true,
            ),
            onClick = {},
        )
    }
}
