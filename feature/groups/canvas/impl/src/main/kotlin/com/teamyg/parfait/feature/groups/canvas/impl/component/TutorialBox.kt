package com.teamyg.parfait.feature.groups.canvas.impl.component

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.R as CoreUiR

/**
 * 튜토리얼 한 장의 설명 카드. 칩이 곧 "다음" 버튼이라 카드 안에 다른 조작부가 없다.
 *
 * 딤 위에 올라가므로 배경을 불투명 White 로 둔다 — 반투명이면 뒤의 목업 이미지가 글자에 비친다.
 */
@Composable
internal fun TutorialBox(
    stepLabel: String,
    title: String,
    description: String,
    onClickStep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(
                color = YGAtomicColors.Gray.White,
            ).padding(
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
            Text(
                text = stepLabel,
                style = YGTheme.typography.caption.c01M,
                color = YGAtomicColors.Gray.Gray500,
                modifier = Modifier.weight(1f),
            )
            YGChipButton(
                text = stringResource(CoreUiR.string.next),
                colors = YGChipButtonColorsDefaults.CherrySolid,
                onClick = onClickStep,
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

private data class TutorialBoxPreviewData(
    val stepLabel: String,
    val title: String,
    val description: String,
)

private class TutorialBoxPreviewParameterProvider : PreviewParameterProvider<TutorialBoxPreviewData> {
    override val values: Sequence<TutorialBoxPreviewData>
        get() = sequenceOf(
            TutorialBoxPreviewData(
                stepLabel = "1/3",
                title = "지난 캔버스 보기",
                description = "상단 날짜 버튼을 눌러\n지난 날의 캔버스를 다시 볼 수 있어요",
            ),
            TutorialBoxPreviewData(
                stepLabel = "2/3",
                title = "캔버스 토핑 추가",
                description = "캔버스 하단 토핑 추가 버튼을 눌러\n카메라 또는 갤러리에서 사진을 업로드 할 수 있어요",
            ),
        )
}

@YGPreview
@Composable
private fun PreviewTutorialBox(
    @PreviewParameter(TutorialBoxPreviewParameterProvider::class) data: TutorialBoxPreviewData,
) = PreviewBox {
    TutorialBox(
        stepLabel = data.stepLabel,
        title = data.title,
        description = data.description,
        onClickStep = {},
        modifier = Modifier.fillMaxWidth(),
    )
}
