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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.R
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
 * 버튼 라벨은 호출부가 정하지 않는다. 마지막 장을 닫는 버튼은 "다음"이 아니라 "시작하기"인데,
 * 그 판단은 [progress] 하나로 끝나므로 화면마다 다시 쓰면 어긋날 자리만 늘어난다.
 *
 * @param progress 여러 장짜리 튜토리얼의 진행. 한 장뿐이면 넘기지 않는다 — 그 한 장이 곧
 *   마지막이라 버튼은 "시작하기"가 되고, 진행 표시가 비운 윗줄은 제목이 올라와 채운다
 */
@Composable
fun YGTutorialBox(
    title: String,
    description: String,
    onClickButton: () -> Unit,
    modifier: Modifier = Modifier,
    progress: YGTutorialProgress? = null,
) {
    val isLastStep = progress?.isLast ?: true

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
            // 진행 표시가 없으면 제목이 그 자리로 올라온다 — 한 장짜리에서 윗줄을 비워 두면
            // 칩만 덩그러니 떠 카드가 위아래로 벌어진다
            if (progress == null) {
                Text(
                    text = title,
                    style = YGTheme.typography.body.b01SB,
                    color = YGAtomicColors.Gray.Gray900,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Text(
                    text = stringResource(R.string.yg_tutorial_step, progress.step, progress.total),
                    style = YGTheme.typography.caption.c01M,
                    color = YGAtomicColors.Gray.Gray500,
                    modifier = Modifier.weight(1f),
                )
            }

            YGChipButton(
                text = stringResource(
                    if (isLastStep) R.string.yg_tutorial_start else R.string.yg_tutorial_next,
                ),
                colors = YGChipButtonColorsDefaults.CherrySolid,
                onClick = onClickButton,
            )
        }

        if (progress != null) {
            Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap3))

            Text(
                text = title,
                style = YGTheme.typography.body.b01SB,
                color = YGAtomicColors.Gray.Gray900,
            )
        }

        // 제목이 윗줄로 올라갔든 아니든, 제목과 설명 사이는 늘 이 간격이다
        Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap2))

        Text(
            text = description,
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray800,
        )
    }
}

private data class YGTutorialBoxPreviewData(
    val progress: YGTutorialProgress?,
    val title: String,
    val description: String,
)

/** 세 갈래를 나란히 둔다 — 중간 장("다음"), 마지막 장, 한 장짜리(둘 다 "시작하기") */
private class YGTutorialBoxPreviewParameterProvider : PreviewParameterProvider<YGTutorialBoxPreviewData> {
    override val values: Sequence<YGTutorialBoxPreviewData>
        get() = sequenceOf(
            YGTutorialBoxPreviewData(
                progress = YGTutorialProgress(step = 1, total = 3),
                title = "지난 캔버스 보기",
                description = "상단 날짜 버튼을 눌러\n지난 날의 캔버스를 다시 볼 수 있어요",
            ),
            YGTutorialBoxPreviewData(
                progress = YGTutorialProgress(step = 3, total = 3),
                title = "캔버스 편집",
                description = "캔버스 편집 버튼을 눌러\n배경과 토핑을 자유롭게 꾸밀 수 있어요",
            ),
            YGTutorialBoxPreviewData(
                progress = null,
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
        title = data.title,
        description = data.description,
        onClickButton = {},
        progress = data.progress,
        modifier = Modifier.fillMaxWidth(),
    )
}
