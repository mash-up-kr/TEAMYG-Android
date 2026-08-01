package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/**
 * 세그멘테이션으로 분리된 객체 이미지를 크게 확인하는 화면.
 *
 * TODO: 디자인 확정 후 문구와 레이아웃 조정 필요
 */
@Composable
internal fun SegmentationConfirmScreen(
    subjectImagePath: String,
    onClickBack: () -> Unit,
    onClickClose: () -> Unit,
    onClickEditPhoto: () -> Unit,
    onClickNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YGAtomicColors.Gray.White),
    ) {
        SegmentationConfirmTopBar(
            onClickBack = onClickBack,
            onClickClose = onClickClose,
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = YGTheme.layout.padding.padding7),
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = subjectImagePath),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = YGTheme.layout.padding.padding7,
                    end = YGTheme.layout.padding.padding7,
                    top = YGTheme.layout.padding.padding5,
                    bottom = YGTheme.layout.padding.padding6,
                ),
        ) {
            YGButton(
                text = "사진 편집", // TODO: string resource 분리 필요
                buttonType = YGButtonType.Medium.Secondary,
                isEnabled = true,
                onClick = onClickEditPhoto,
                modifier = Modifier.weight(1f),
            )
            YGButton(
                text = "다음", // TODO: string resource 분리 필요
                buttonType = YGButtonType.Medium.Primary,
                isEnabled = true,
                onClick = onClickNext,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SegmentationConfirmTopBar(
    onClickBack: () -> Unit,
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = YGTheme.layout.padding.padding7,
                end = YGTheme.layout.padding.padding7,
                top = YGTheme.layout.padding.padding6,
            ),
    ) {
        YGCircleButton(
            iconResource = DesignSystemR.drawable.ic_caret_left,
            type = YGCircleButtonType.Default,
            contentDescription = "뒤로가기",
            onClick = onClickBack,
        )
        YGCircleButton(
            iconResource = DesignSystemR.drawable.ic_close,
            type = YGCircleButtonType.Default,
            contentDescription = "닫기",
            onClick = onClickClose,
        )
    }
}

@YGPreview
@Composable
private fun SegmentationConfirmScreenPreview() = PreviewBox {
    SegmentationConfirmScreen(
        subjectImagePath = "",
        onClickBack = {},
        onClickClose = {},
        onClickEditPhoto = {},
        onClickNext = {},
    )
}
