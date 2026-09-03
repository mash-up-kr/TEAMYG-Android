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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarTitle
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.jvm.model.DateTextFormat
import com.teamyg.parfait.feature.groups.canvas.impl.R
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format

/**
 * 캔버스를 갤러리에 넣기 전, 무엇이 저장될지 그대로 보여 주는 화면.
 *
 * 저장 자체는 하지 않는다 — 확정을 호출부에 알리기만 하고, 갤러리에 넣는 일과 결과를 알리는
 * 일은 캔버스 메인이 맡는다.
 *
 * @param imagePath 캔버스 메인이 캡처해 캐시에 구운 PNG 의 경로
 */
@Composable
internal fun CanvasImageSaveScreen(
    imagePath: String,
    date: LocalDate,
    onClickClose: () -> Unit,
    onClickSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YGAtomicColors.Gray.White),
    ) {
        YGFloatingBarTitle(
            title = stringResource(R.string.canvas_image_save_title),
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
                    .border(width = (0.59).dp, color = YGAtomicColors.Gray.Gray500)
                    .padding(horizontal = YGTheme.layout.padding.padding7),
            ) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = stringResource(R.string.canvas_image_save_preview_content_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
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
                text = stringResource(R.string.canvas_image_save_description),
                style = YGTheme.typography.caption.c01M,
                color = YGAtomicColors.Gray.Gray500,
                textAlign = TextAlign.Center,
            )
        }

        Box(
            contentAlignment = Alignment.Center,
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
                text = stringResource(R.string.canvas_image_save_confirm),
                buttonType = YGButtonType.Medium.Primary,
                isEnabled = true,
                onClick = onClickSave,
            )
        }
    }
}

private class CanvasImageSaveScreenPreviewParameterProvider : PreviewParameterProvider<LocalDate> {
    override val values: Sequence<LocalDate>
        get() = sequenceOf(
            LocalDate(2026, 5, 20),
            // 마감된 지난 캔버스도 같은 화면으로 저장한다
            LocalDate(2026, 5, 3),
        )
}

@YGPreview
@Composable
private fun PreviewCanvasImageSaveScreen(
    @PreviewParameter(CanvasImageSaveScreenPreviewParameterProvider::class) date: LocalDate,
) = PreviewBox {
    CanvasImageSaveScreen(
        imagePath = "",
        date = date,
        onClickClose = {},
        onClickSave = {},
        modifier = Modifier.fillMaxSize(),
    )
}
