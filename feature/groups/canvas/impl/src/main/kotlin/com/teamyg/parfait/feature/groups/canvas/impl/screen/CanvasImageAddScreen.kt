package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasToppingLayer
import com.teamyg.parfait.feature.groups.canvas.impl.component.CustomCalendar
import com.teamyg.parfait.core.designsystem.component.ygbackgrounddotgrid.ygBackgroundDotGrid
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvas
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuAction
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuItem
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChip
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChipStyle
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarCanvas
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasImageAddUiState
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.GroupMemberChip
import kotlinx.datetime.LocalDate
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

private const val MAX_VISIBLE_MEMBER_CHIPS = 5

/** [YGCanvas] 가 배경을 안 받았을 때 쓰는 값과 같다 */
private val DEFAULT_CANVAS_BACKGROUND = YGCanvasBackground.Solid(YGAtomicColors.Gray.Gray100)

@Composable
internal fun CanvasImageAddScreen(
    canvasState: CanvasImageAddUiState,
    onClickBack: () -> Unit,
    onClickDateSelect: () -> Unit,
    onClickMenu: () -> Unit,
    onClickCamera: () -> Unit,
    onClickGallery: () -> Unit,
    onClickEditCanvasBG: () -> Unit,
    onDismissCalendar: () -> Unit,
    onSelectYear: (Int) -> Unit,
    onSelectMonth: (LocalDate) -> Unit,
    onClickDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val openMenu = { isMenuExpanded = true }

    Column(
        modifier = modifier
            .background(YGAtomicColors.Gray.White)
            .ygBackgroundDotGrid(),
    ) {
        YGTopBarCanvas(
            title = canvasState.groupName,
            onBackClick = onClickBack,
            onMenuClick = onClickMenu,
            memberContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(-12.dp)) {
                    canvasState.memberChips.take(MAX_VISIBLE_MEMBER_CHIPS).forEach { member ->
                        YGNametagChip(
                            colorChipType = member.colorChipType,
                            userFirstName = member.nickname.take(1),
                            chip = YGNametagChipStyle.Style28,
                        )
                    }

                    val overflowCount = canvasState.memberChips.size - MAX_VISIBLE_MEMBER_CHIPS
                    if (overflowCount > 0) {
                        YGNametagChip(
                            colorChipType = YGColorChipType.NametagChipPlus,
                            userFirstName = stringResource(
                                R.string.canvas_image_add_member_overflow_count,
                                overflowCount,
                            ),
                            chip = YGNametagChipStyle.Style28,
                        )
                    }
                }
            },
        )

        YGCanvas(
            date = canvasState.canvasDate,
            day = "(${canvasState.canvasDay})",
            onDateSelectClick = onClickDateSelect,
            addAction = YGCanvasMenuAction(
                text = stringResource(R.string.canvas_image_add_topping_add),
                iconResource = DesignSystemR.drawable.ic_plus,
                onClick = openMenu,
            ),
            editAction = YGCanvasMenuAction(
                text = stringResource(R.string.canvas_image_add_canvas_edit),
                iconResource = DesignSystemR.drawable.ic_caret_right,
                onClick = onClickEditCanvasBG,
            ),
            background = canvasState.canvasBackground.toYGCanvasBackground(),
            isEmpty = canvasState.isCanvasEmpty,
            emptyMessage = stringResource(R.string.canvas_image_add_empty_message),
            // 메뉴와 캘린더 모두 캔버스를 가린 채 뜬다 — 어느 쪽이든 바깥을 누르면 닫힌다
            isDimmed = isMenuExpanded || canvasState.isCalendarVisible,
            onDimClick = {
                isMenuExpanded = false
                onDismissCalendar()
            },
            isMenuExpanded = isMenuExpanded,
            isCalendarVisible = canvasState.isCalendarVisible,
            calendarContent = {
                CustomCalendar(
                    displayedMonth = canvasState.displayedMonth,
                    today = canvasState.today,
                    selectedDate = canvasState.selectedDate,
                    uploadedDates = canvasState.uploadedDates,
                    selectableYears = canvasState.selectableYears,
                    selectableMonths = canvasState.selectableMonths,
                    onSelectYear = onSelectYear,
                    onSelectMonth = onSelectMonth,
                    onClickDate = onClickDate,
                )
            },
            expandedItems = listOf(
                YGCanvasMenuItem(
                    text = stringResource(R.string.canvas_image_add_camera_capture),
                    onClick = {
                        isMenuExpanded = false
                        onClickCamera()
                    },
                ),
                YGCanvasMenuItem(
                    text = stringResource(R.string.canvas_image_add_gallery_select),
                    onClick = {
                        isMenuExpanded = false
                        onClickGallery()
                    },
                ),
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            CanvasToppingLayer(
                toppings = canvasState.toppings,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * 배경이 미설정이거나 앱이 모르는 type 이면 null 이 온다. 색을 못 읽었을 때도 마찬가지로,
 * 셋 다 기본 배경으로 떨어뜨린다 — 캔버스를 못 그리는 것보다 낫다.
 */
private fun CanvasBackground?.toYGCanvasBackground(): YGCanvasBackground = when (this) {
    null -> DEFAULT_CANVAS_BACKGROUND

    is CanvasBackground.Color ->
        value
            .toColorOrNull()
            ?.let(YGCanvasBackground::Solid)
            ?: DEFAULT_CANVAS_BACKGROUND

    is CanvasBackground.Image -> YGCanvasBackground.Image(url)
}

private class CanvasImageAddScreenPreviewParameterProvider :
    PreviewParameterProvider<CanvasImageAddUiState> {
    override val values: Sequence<CanvasImageAddUiState>
        get() = sequenceOf(
            CanvasImageAddUiState(
                groupName = "그룹이름은최대열글자",
                memberChips = listOf(
                    GroupMemberChip("문어", YGColorChipType.NametagChip1),
                    GroupMemberChip("전봇대", YGColorChipType.NametagChip8),
                    GroupMemberChip("김밥", YGColorChipType.NametagChip5),
                    GroupMemberChip("장미", YGColorChipType.NametagChip3),
                    GroupMemberChip("김치", YGColorChipType.NametagChip11),
                    GroupMemberChip("류현진", YGColorChipType.NametagChip6),
                    GroupMemberChip("정거장", YGColorChipType.NametagChip2),
                ),
                canvasDate = "May 20",
                canvasDay = "Wed",
            ),
        )
}

@YGPreview
@Composable
private fun PreviewCanvasImageAddScreen(
    @PreviewParameter(CanvasImageAddScreenPreviewParameterProvider::class) uiState: CanvasImageAddUiState,
) = PreviewBox {
    CanvasImageAddScreen(
        canvasState = uiState,
        onClickBack = {},
        onClickDateSelect = {},
        onClickMenu = {},
        onClickCamera = {},
        onClickGallery = {},
        onClickEditCanvasBG = {},
        onDismissCalendar = {},
        onSelectYear = {},
        onSelectMonth = {},
        onClickDate = {},
        modifier = Modifier.fillMaxSize(),
    )
}
