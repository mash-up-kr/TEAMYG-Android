package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

sealed interface YGToastType {
    // data object
    data class InviteCode(val text: String) : YGToastType

    data class Edit(val text: String) : YGToastType

    data class Record(val userName: String, val time: String) : YGToastType
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
            .height(41.dp)
            .background(color = YGAtomicColors.Transparency.Black75)
            .padding(start = 16.dp),
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

@Preview
@Composable
private fun YGToastPreview() {
    YGCustomTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            YGToast(
                type = YGToastType.Record("파르페", "12분"),
            )
            YGToast(
                type = YGToastType.Edit("내 토핑만 편집할 수 있어요"),
            )
            YGToast(
                type = YGToastType.InviteCode("초대 코드를 복사했어요"),
            )
        }
    }
}
