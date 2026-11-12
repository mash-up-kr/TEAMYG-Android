package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarTitle
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.jvm.model.DateTextFormat
import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveType
import com.teamyg.parfait.feature.groups.canvas.impl.R
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format

/**
 * 캔버스를 갤러리에 넣기 전, 무엇이 저장될지 그대로 보여 주는 화면. [saveType] 으로 사진·영상
 * 미리보기를 겸한다 — 틀은 같고 [preview] 영역과 제목·설명·확정 버튼 문구만 갈린다.
 *
 * 저장 자체는 하지 않는다 — 확정을 호출부에 알리기만 하고, 갤러리에 넣는 일과 결과를 알리는
 * 일은 캔버스 메인이 맡는다.
 *
 * @param isActionEnabled 저장·공유를 눌러 결과를 기다리는 동안 거짓이다
 * @param preview 미리보기 상자에 채울 내용. 사진이면 정적 이미지, 영상이면 타임랩스 반복 재생
 */
@Composable
internal fun CanvasImageSaveScreen(
    saveType: CanvasSaveType,
    date: LocalDate,
    isActionEnabled: Boolean,
    onClickClose: () -> Unit,
    onClickSave: () -> Unit,
    onClickShareInstagram: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    val isVideo = saveType == CanvasSaveType.VIDEO

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YGAtomicColors.Gray.White),
    ) {
        YGFloatingBarTitle(
            title = stringResource(
                if (isVideo) R.string.canvas_video_save_title else R.string.canvas_image_save_title,
            ),
            onCloseClick = onClickClose,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(YGTheme.layout.padding.padding7),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(width = 198.dp)
                    .aspectRatio(CANVAS_AREA_ASPECT_RATIO)
                    .border(width = (0.59).dp, color = YGAtomicColors.Gray.Gray500),
            ) {
                preview()
            }

            Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap7))

            Row(
                horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = date.format(DateTextFormat.monthDayFormat),
                    style = YGTheme.typography.body.b02R,
                    color = YGAtomicColors.Gray.Gray900,
                )

                Text(
                    text = "(${date.format(DateTextFormat.weekdayFormat)})",
                    style = YGTheme.typography.body.b02R,
                    color = YGAtomicColors.Gray.Gray300,
                )

                Text(
                    text = stringResource(R.string.canvas_image_save_question_suffix),
                    style = YGTheme.typography.body.b02R,
                    color = YGAtomicColors.Gray.Gray900,
                )
            }

            Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap3))

            Text(
                text = stringResource(
                    if (isVideo) R.string.canvas_video_save_description else R.string.canvas_image_save_description,
                ),
                style = YGTheme.typography.caption.c01M,
                color = YGAtomicColors.Gray.Gray500,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = YGTheme.layout.padding.padding7,
                    top = YGTheme.layout.padding.padding6,
                    end = YGTheme.layout.padding.padding7,
                    bottom = YGTheme.layout.padding.padding7,
                ),
        ) {
            YGButton(
                text = stringResource(
                    if (isVideo) R.string.canvas_video_save_confirm else R.string.canvas_image_save_confirm,
                ),
                buttonType = YGButtonType.Large,
                isEnabled = isActionEnabled,
                onClick = onClickSave,
                modifier = Modifier.fillMaxWidth(),
            )

            YGButton(
                text = stringResource(R.string.canvas_save_share_instagram),
                buttonType = YGButtonType.Medium.Secondary,
                isEnabled = isActionEnabled,
                onClick = onClickShareInstagram,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private class CanvasImageSaveScreenPreviewParameterProvider : NamedPreviewParameterProvider<CanvasSaveType>() {
    override val entries = listOf(
        "image" to CanvasSaveType.IMAGE,
        "video" to CanvasSaveType.VIDEO,
    )
}

@YGPreview
@Composable
private fun PreviewCanvasImageSaveScreen(
    @PreviewParameter(CanvasImageSaveScreenPreviewParameterProvider::class) saveType: CanvasSaveType,
) = PreviewBox {
    CanvasImageSaveScreen(
        saveType = saveType,
        date = LocalDate(2026, 5, 20),
        isActionEnabled = true,
        onClickClose = {},
        onClickSave = {},
        onClickShareInstagram = {},
        modifier = Modifier.fillMaxSize(),
        preview = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(YGAtomicColors.Gray.Gray100),
            )
        },
    )
}
