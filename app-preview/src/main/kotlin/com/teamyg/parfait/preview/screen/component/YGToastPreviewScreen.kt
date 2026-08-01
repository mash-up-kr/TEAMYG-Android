package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToast
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private data class YGToastSample(
    val label: String,
    val type: YGToastType,
)

private val ygToastSamples: List<YGToastSample> = listOf(
    YGToastSample(
        label = "Record (Figma Type=Alert)",
        type = YGToastType.Record(userName = "WWWWWWWWWW", time = "59분"),
    ),
    YGToastSample(
        label = "Edit (Figma Type=Warning)",
        type = YGToastType.Edit("내 토핑만 편집할 수 있어요"),
    ),
    YGToastSample(
        label = "InviteCode (Figma Type=Success)",
        type = YGToastType.InviteCode("초대 코드를 복사했어요"),
    ),
    YGToastSample(
        label = "Fail (Figma Type=Error)",
        type = YGToastType.Fail("갤러리 저장에 실패했어요. 나중에 다시 시도해 주세요."),
    ),
)

@Composable
internal fun YGToastPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toastPolicy = rememberYGToastPolicy()

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarBack(onIconClick = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ygToastSamples) { sample ->
                    PreviewSection(sample.label) {
                        YGToast(type = sample.type)
                    }
                }
                items(ygToastSamples) { sample ->
                    PreviewSection("show: ${sample.label}") {
                        YGButton(
                            text = "띄우기",
                            buttonType = YGButtonType.Medium.Primary,
                            isEnabled = true,
                            onClick = { toastPolicy.show(sample.type) },
                        )
                    }
                }
            }
        }
        YGToastHost(
            policy = toastPolicy,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        )
    }
}

@YGPreview
@Composable
private fun PreviewYGToastPreviewScreen() = PreviewBox {
    YGToastPreviewScreen(
        onBack = {},
    )
}
