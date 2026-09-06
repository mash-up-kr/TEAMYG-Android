package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.ygtutorial.YGTutorialOverlay
import com.teamyg.parfait.core.designsystem.component.ygtutorial.YGTutorialProgress
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasTutorialStep

/**
 * 앱 설치 후 캔버스 첫 진입에서 한 번 도는 튜토리얼. 겹치는 방식은 [YGTutorialOverlay] 가 정하고,
 * 여기서는 [CanvasTutorialStep] 이 들고 있는 장별 자료를 그 자리에 꽂아 넣기만 한다.
 */
@Composable
internal fun CanvasTutorialOverlay(
    step: CanvasTutorialStep,
    onClickNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGTutorialOverlay(
        imageResource = step.imageResource,
        title = stringResource(step.titleResource),
        description = stringResource(step.descriptionResource),
        onClickButton = onClickNext,
        progress = YGTutorialProgress(
            step = step.stepNumber,
            total = CanvasTutorialStep.totalCount,
        ),
        placement = step.boxPlacement,
        modifier = modifier,
    )
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
