package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasTutorialStep
import com.teamyg.parfait.feature.groups.canvas.impl.model.TutorialBoxPlacement

/**
 * 앱 설치 후 캔버스 첫 진입에서 한 번 도는 튜토리얼. 캔버스 화면 **전체**를 덮으며, 시스템바
 * 아래까지 딤이 이어지도록 인셋을 받지 않는다.
 *
 * 목업 이미지는 시스템바를 뺀 자리에만 그린다 — 이미지가 앱 상단바부터 시작하는 한 장이라,
 * 상태바까지 끌어올리면 실제 화면보다 위로 밀려 어긋나 보인다.
 *
 * 딤 자체가 클릭을 삼킨다. 튜토리얼이 떠 있는 동안 그 아래 캔버스가 눌리면, 사용자는 자기가
 * 무엇을 건드렸는지 모른 채 다른 화면에 가 있게 된다.
 *
 * 설명 카드가 붙는 변은 장마다 다르다([CanvasTutorialStep.boxPlacement]). 어느 쪽이든 시스템바
 * 인셋은 카드만 받는다 — 딤은 화면 끝까지 이어지고 글자만 시스템바를 피한다.
 */
@Composable
internal fun CanvasTutorialOverlay(
    step: CanvasTutorialStep,
    onClickNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YGAtomicColors.Transparency.Black50)
            .clickableYGNoRipple(onClick = {}),
    ) {
        Image(
            painter = painterResource(id = step.imageResource),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        )

        val isBottomPlacement = step.boxPlacement == TutorialBoxPlacement.Bottom

        TutorialBox(
            stepLabel = stringResource(
                R.string.canvas_tutorial_step,
                step.stepNumber,
                CanvasTutorialStep.totalCount,
            ),
            title = stringResource(step.titleResource),
            description = stringResource(step.descriptionResource),
            onClickStep = onClickNext,
            modifier = Modifier
                .align(if (isBottomPlacement) Alignment.BottomCenter else Alignment.TopCenter)
                .windowInsetsPadding(
                    if (isBottomPlacement) WindowInsets.navigationBars else WindowInsets.statusBars,
                ).padding(
                    top = if (isBottomPlacement) 0.dp else YGTheme.layout.padding.padding6,
                    bottom = if (isBottomPlacement) YGTheme.layout.padding.padding1 else 0.dp,
                    start = YGTheme.layout.padding.padding7,
                    end = YGTheme.layout.padding.padding7,
                ).fillMaxWidth(),
        )
    }
}

private class CanvasTutorialStepPreviewParameterProvider : PreviewParameterProvider<CanvasTutorialStep> {
    override val values: Sequence<CanvasTutorialStep>
        get() = CanvasTutorialStep.entries.asSequence()
}

@YGPreview
@Composable
private fun PreviewCanvasTutorialOverlay(
    @PreviewParameter(CanvasTutorialStepPreviewParameterProvider::class) step: CanvasTutorialStep,
) = PreviewBox {
    CanvasTutorialOverlay(
        step = step,
        onClickNext = {},
        modifier = Modifier.fillMaxSize(),
    )
}
