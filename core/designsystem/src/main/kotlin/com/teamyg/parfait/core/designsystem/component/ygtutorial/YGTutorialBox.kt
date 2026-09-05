package com.teamyg.parfait.core.designsystem.component.ygtutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * 튜토리얼 한 장의 설명 카드. 칩이 곧 진행 버튼이라 카드 안에 다른 조작부가 없다.
 *
 * 딤 위에 올라가므로 배경을 불투명 White 로 둔다 — 반투명이면 뒤의 목업 이미지가 글자에 비친다.
 *
 * @param stepLabel 여러 장짜리 튜토리얼의 진행 표시(`1/3`). 한 장뿐이면 넘기지 않는다 —
 *   그때는 그 자리가 비고 칩만 오른쪽에 남는다
 */
@Composable
fun YGTutorialBox(
    buttonText: String,
    title: String,
    description: String,
    onClickButton: () -> Unit,
    modifier: Modifier = Modifier,
    stepLabel: String? = null,
) {
    Column(
        modifier = modifier
            .background(color = YGAtomicColors.Gray.White)
            .padding(
                top = YGTheme.layout.padding.padding5,
                end = YGTheme.layout.padding.padding5,
                bottom = YGTheme.layout.padding.padding6,
                start = YGTheme.layout.padding.padding7,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (stepLabel == null) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Text(
                    text = stepLabel,
                    style = YGTheme.typography.caption.c01M,
                    color = YGAtomicColors.Gray.Gray500,
                    modifier = Modifier.weight(1f),
                )
            }

            YGChipButton(
                text = buttonText,
                colors = YGChipButtonColorsDefaults.CherrySolid,
                onClick = onClickButton,
            )
        }

        Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap3))

        Text(
            text = title,
            style = YGTheme.typography.body.b01SB,
            color = YGAtomicColors.Gray.Gray900,
        )

        Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap2))

        Text(
            text = description,
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray800,
        )
    }
}

private data class YGTutorialBoxPreviewData(
    val stepLabel: String?,
    val title: String,
    val description: String,
)

private class YGTutorialBoxPreviewParameterProvider : PreviewParameterProvider<YGTutorialBoxPreviewData> {
    override val values: Sequence<YGTutorialBoxPreviewData>
        get() = sequenceOf(
            YGTutorialBoxPreviewData(
                stepLabel = "1/3",
                title = "지난 캔버스 보기",
                description = "상단 날짜 버튼을 눌러\n지난 날의 캔버스를 다시 볼 수 있어요",
            ),
            YGTutorialBoxPreviewData(
                stepLabel = null,
                title = "오늘의 사진 업로드",
                description = "매일의 순간을 그대로 기록할 수 있도록\n오늘 찍은 사진만 업로드할 수 있어요",
            ),
        )
}

@YGPreview
@Composable
private fun YGTutorialBoxPreview(
    @PreviewParameter(YGTutorialBoxPreviewParameterProvider::class) data: YGTutorialBoxPreviewData,
) = PreviewBox {
    YGTutorialBox(
        buttonText = "다음",
        title = data.title,
        description = data.description,
        onClickButton = {},
        stepLabel = data.stepLabel,
        modifier = Modifier.fillMaxWidth(),
    )
}
