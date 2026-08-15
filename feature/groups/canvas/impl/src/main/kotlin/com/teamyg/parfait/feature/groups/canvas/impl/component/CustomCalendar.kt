package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.core.designsystem.component.etc.YGHorizontalDivider
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.component.yglistdate.YGListDate
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.jvm.model.DateTextFormat
import com.teamyg.parfait.feature.groups.canvas.impl.R
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

private const val DAYS_IN_WEEK = 7

internal data class CalendarDayUiModel(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
)

@Composable
internal fun CustomCalendar(
    displayedMonth: LocalDate,
    today: LocalDate,
    selectedDate: LocalDate?,
    uploadedDates: Set<LocalDate>,
    onClickMonth: () -> Unit,
    onClickYear: () -> Unit,
    onClickDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstDayOfMonth = remember(displayedMonth) { displayedMonth.toFirstDayOfMonth() }
    val days = remember(firstDayOfMonth) { buildCalendarDays(firstDayOfMonth) }

    Column(
        modifier = modifier
            .background(YGAtomicColors.Gray.White)
            .sideBorder(
                width = SizeTokens.Size1.getDp(),
                color = YGAtomicColors.Gray.Gray500,
            ),
    ) {
        HeadCalendar(
            month = firstDayOfMonth.format(DateTextFormat.monthFormat),
            year = firstDayOfMonth.year.toString(),
            onClickMonth = onClickMonth,
            onClickYear = onClickYear,
        )

        YGHorizontalDivider()

        BodyCalendar(
            days = days,
            today = today,
            selectedDate = selectedDate,
            uploadedDates = uploadedDates,
            onClickDate = onClickDate,
        )
    }
}

/**
 * 좌·우·하단에만 테두리를 그린다. 상단을 빼는 이유는 바로 위에 붙는
 * `YGCanvasDateSelectButton` 이 같은 1dp Gray500 테두리를 이미 두르고 있어서,
 * 그대로 두면 맞닿는 자리에 선이 두 겹으로 깔린다.
 *
 * `Modifier.border` 는 변을 골라 그릴 수 없어 직접 그린다.
 */
private fun Modifier.sideBorder(
    width: Dp,
    color: Color,
): Modifier = drawBehind {
    val strokeWidth = width.toPx()
    // 선의 중심이 경계에 놓이면 절반이 밖으로 잘려 두께가 얇아 보인다
    val inset = strokeWidth / 2

    drawLine(color, Offset(inset, 0f), Offset(inset, size.height), strokeWidth)
    drawLine(color, Offset(size.width - inset, 0f), Offset(size.width - inset, size.height), strokeWidth)
    drawLine(color, Offset(0f, size.height - inset), Offset(size.width, size.height - inset), strokeWidth)
}

private fun LocalDate.toFirstDayOfMonth(): LocalDate = minus(DatePeriod(days = day - 1))

/**
 * 앞은 이전 달, 뒤는 다음 달 날짜로 채워 항상 7의 배수를 만든다.
 *
 * 빈 칸이 아니라 실제 날짜를 넣어야 칸마다 오늘·선택·업로드 여부를 스스로 판단할 수 있다.
 */
private fun buildCalendarDays(firstDayOfMonth: LocalDate): List<CalendarDayUiModel> {
    val lastDayOfMonth = firstDayOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))

    // 머리글이 일요일부터라 일요일을 0 으로 맞춘다 (ISO 는 월요일이 1, 일요일이 7)
    val leadingDayCount = firstDayOfMonth.dayOfWeek.isoDayNumber % DAYS_IN_WEEK
    val gridStartDate = firstDayOfMonth.minus(DatePeriod(days = leadingDayCount))

    val cellCount = ceilToWeek(leadingDayCount + lastDayOfMonth.day)

    return List(cellCount) { index ->
        val date = gridStartDate.plus(DatePeriod(days = index))
        CalendarDayUiModel(
            date = date,
            isCurrentMonth = date in firstDayOfMonth..lastDayOfMonth,
        )
    }
}

private fun ceilToWeek(dayCount: Int): Int = (dayCount + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK * DAYS_IN_WEEK

@Composable
private fun HeadCalendar(
    month: String,
    year: String,
    onClickMonth: () -> Unit,
    onClickYear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(
            start = YGTheme.layout.padding.padding6,
            top = YGTheme.layout.padding.padding3,
            end = YGTheme.layout.padding.padding6,
            bottom = YGTheme.layout.padding.padding2,
        ),
    ) {
        CalendarPeriodSelector(
            text = month,
            onClick = onClickMonth,
        )
        CalendarPeriodSelector(
            text = year,
            onClick = onClickYear,
        )
    }
}

/**
 * 텍스트와 캐럿이 한 덩어리다. `interactionSource` 까지 공유해야 텍스트를 눌렀을 때도
 * 캐럿이 같이 눌린 색이 된다 — 따로 두면 별개의 버튼 두 개로 보인다.
 */
@Composable
private fun CalendarPeriodSelector(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable(
            onClick = onClick,
            indication = null,
            interactionSource = interactionSource,
        ),
    ) {
        Text(
            text = text,
            style = YGTheme.typography.title.t03SB,
            color = YGAtomicColors.Gray.Gray900,
        )
        YGIconButton(
            iconResource = DesignSystemR.drawable.ic_caret_bottom,
            size = YGIconButtonSize.SIZE_44,
            contentDescription = null,
            onClick = onClick,
            interactionSource = interactionSource,
        )
    }
}

@Composable
private fun BodyCalendar(
    days: List<CalendarDayUiModel>,
    today: LocalDate,
    selectedDate: LocalDate?,
    uploadedDates: Set<LocalDate>,
    onClickDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
        modifier = modifier.padding(
            start = YGTheme.layout.padding.padding6,
            top = YGTheme.layout.padding.padding2,
            end = YGTheme.layout.padding.padding6,
            bottom = YGTheme.layout.padding.padding5,
        ),
    ) {
        DayOfWeekHeader()

        days.chunked(DAYS_IN_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    YGListDate(
                        text = day.date.day.toString(),
                        isSelected = day.date == selectedDate,
                        isToday = day.date == today,
                        // 앞뒤 달 날짜는 보여주되 고를 수는 없다
                        isEnabled = day.isCurrentMonth,
                        isUploaded = day.date in uploadedDates,
                        onClick = { onClickDate(day.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayOfWeekHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        stringArrayResource(R.array.canvas_calendar_day_of_week).forEach { dayOfWeek ->
            Text(
                text = dayOfWeek,
                style = YGTheme.typography.body.b02R,
                color = YGAtomicColors.Gray.Gray400,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = YGTheme.layout.padding.padding4),
            )
        }
    }
}

private data class CustomCalendarPreviewData(
    val displayedMonth: LocalDate,
    val today: LocalDate,
    val selectedDate: LocalDate?,
    val uploadedDates: Set<LocalDate>,
)

/** 줄 수가 고정이 아님을 바로 보도록 주 수가 갈리는 달을 고른다 */
private class CustomCalendarPreviewParameterProvider : PreviewParameterProvider<CustomCalendarPreviewData> {
    override val values: Sequence<CustomCalendarPreviewData>
        get() = sequenceOf(
            // 6주 — 토요일 시작 31일. 앞뒤 달 날짜가 모두 나온다
            CustomCalendarPreviewData(
                displayedMonth = LocalDate(2026, 8, 1),
                today = LocalDate(2026, 8, 15),
                selectedDate = LocalDate(2026, 8, 20),
                uploadedDates = setOf(
                    LocalDate(2026, 8, 3),
                    LocalDate(2026, 8, 15),
                    LocalDate(2026, 8, 20),
                ),
            ),
            // 4주 — 일요일 시작 28일. 앞뒤 달 날짜가 하나도 없다
            CustomCalendarPreviewData(
                displayedMonth = LocalDate(2026, 2, 1),
                today = LocalDate(2026, 8, 15),
                selectedDate = null,
                uploadedDates = setOf(LocalDate(2026, 2, 14)),
            ),
            // 5주 — 오늘·선택·업로드가 모두 없는 기본 상태
            CustomCalendarPreviewData(
                displayedMonth = LocalDate(2026, 11, 1),
                today = LocalDate(2026, 8, 15),
                selectedDate = null,
                uploadedDates = emptySet(),
            ),
        )
}

@YGPreview
@Composable
private fun CustomCalendarPreview(
    @PreviewParameter(CustomCalendarPreviewParameterProvider::class) data: CustomCalendarPreviewData,
) = PreviewBox {
    CustomCalendar(
        displayedMonth = data.displayedMonth,
        today = data.today,
        selectedDate = data.selectedDate,
        uploadedDates = data.uploadedDates,
        onClickMonth = {},
        onClickYear = {},
        onClickDate = {},
        modifier = Modifier.fillMaxWidth(),
    )
}
