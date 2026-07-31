package com.teamyg.parfait.core.designsystem.component.ygtoast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

sealed interface YGToastType {
    /**
     * Figma Type Success
     */
    data class InviteCode(val text: String) : YGToastType

    /**
     * Figma Type Warning
     */
    data class Edit(val text: String) : YGToastType

    /**
     * Figma Type Alert
     */
    data class Record(val userName: String, val time: String) : YGToastType

    /**
     * Figma Type Error
     */
    data class Fail(val text: String) : YGToastType
}

@Composable
fun YGToast(
    type: YGToastType,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .background(color = YGAtomicColors.Transparency.Black75)
            .padding(
                vertical = YGTheme.layout.padding.padding5,
                horizontal = YGTheme.layout.padding.padding6,
            ),
    ) {
        when (type) {
            is YGToastType.InviteCode -> Text(
                text = type.text,
                style = YGTheme.typography.body.b02SB,
                color = YGAtomicColors.Melon.Melon600,
            )

            is YGToastType.Edit -> Text(
                text = type.text,
                style = YGTheme.typography.body.b02SB,
                color = YGAtomicColors.Pudding.Pudding600,
            )

            is YGToastType.Fail -> Text(
                text = type.text,
                style = YGTheme.typography.body.b02SB,
                color = YGAtomicColors.Cherry.Cherry500,
            )

            is YGToastType.Record -> {
                val userStyle = YGTheme.typography.body.b02SB
                val timeStyle = YGTheme.typography.body.b02R
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            userStyle.toSpanStyle().copy(color = YGAtomicColors.Pudding.Pudding500),
                        ) { append(type.userName) }
                        withStyle(
                            timeStyle.toSpanStyle().copy(color = YGAtomicColors.Gray.Gray100),
                        ) { append("님이 ${type.time} 전에 쌓았어요") }
                    },
                )
            }
        }
    }
}

@YGPreview
@Composable
private fun YGToastPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        YGToast(type = YGToastType.Record(userName = "WWWWWWWWWW", time = "59분"))
        YGToast(type = YGToastType.Edit("내 토핑만 편집할 수 있어요"))
        YGToast(type = YGToastType.InviteCode("초대 코드를 복사했어요"))
        YGToast(type = YGToastType.Fail("갤러리 저장에 실패했어요. 나중에 다시 시도해 주세요."))
    }
}
