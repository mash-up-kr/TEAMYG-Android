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
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasToppingLayer
import com.teamyg.parfait.feature.groups.canvas.impl.component.CustomCalendar
import com.teamyg.parfait.core.designsystem.component.ygalert.YGAlertHost
import com.teamyg.parfait.core.designsystem.component.ygalert.YGAlertPolicy
import com.teamyg.parfait.core.designsystem.component.ygalert.rememberYGAlertPolicy
import com.teamyg.parfait.core.designsystem.component.ygbackgrounddotgrid.ygBackgroundDotGrid
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvas
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuAction
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuItem
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChip
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChipStyle
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarCanvas
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainUiState
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.GroupMemberChip
import kotlinx.datetime.LocalDate
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

private const val MAX_VISIBLE_MEMBER_CHIPS = 5

@Composable
internal fun CanvasMainScreen(
    canvasState: CanvasMainUiState,
    onClickBack: () -> Unit,
    onClickDateSelect: () -> Unit,
    onClickMenu: () -> Unit,
    onClickCamera: () -> Unit,
    onClickGallery: () -> Unit,
    onClickEditCanvasBG: () -> Unit,
    onClickSaveToGallery: () -> Unit,
    onClickGoToToday: () -> Unit,
    onDismissCalendar: () -> Unit,
    onSelectYear: (Int) -> Unit,
    onSelectMonth: (LocalDate) -> Unit,
    onClickDate: (LocalDate) -> Unit,
    onClickTopping: (CanvasToppingVO) -> Unit,
    onClickSpotlightDim: () -> Unit,
    modifier: Modifier = Modifier,
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
    toastPolicy: YGToastPolicy = rememberYGToastPolicy(),
    alertPolicy: YGAlertPolicy = rememberYGAlertPolicy(),
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val openMenu = { isMenuExpanded = true }

    Column(
        modifier = modifier
            .fillMaxSize()
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
                                R.string.canvas_main_member_overflow_count,
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
            // 지난 캔버스는 고치는 자리가 아니다 — 서버가 409 로 거부하므로, 실패를 보여 주기 전에
            // 편집으로 가는 길 자체를 여기서 치운다. 저장도 날짜 버튼 옆 아이콘이 대신하므로
            // 지난 캔버스에서는 아래 메뉴에 더 넣을 자리가 없다
            addAction = if (canvasState.isViewingToday) {
                YGCanvasMenuAction(
                    text = stringResource(R.string.canvas_main_topping_add),
                    iconResource = DesignSystemR.drawable.ic_plus,
                    onClick = openMenu,
                    isEnabled = canvasState.isToppingAddEnabled,
                )
            } else {
                null
            },
            editAction = if (canvasState.isViewingToday) {
                YGCanvasMenuAction(
                    text = stringResource(R.string.canvas_main_canvas_edit),
                    iconResource = DesignSystemR.drawable.ic_caret_right,
                    onClick = onClickEditCanvasBG,
                )
            } else {
                YGCanvasMenuAction(
                    text = stringResource(R.string.canvas_main_go_to_today),
                    iconResource = DesignSystemR.drawable.ic_caret_right,
                    onClick = onClickGoToToday,
                )
            },
            background = canvasState.canvasBackground.toYGCanvasBackground(),
            isSaveVisible = canvasState.isCanvasSaveVisible,
            onClickSave = onClickSaveToGallery,
            saveContentDescription = stringResource(R.string.canvas_main_save_to_gallery),
            captureGraphicsLayer = graphicsLayer,
            // TODO: 전일 캔버스 알림 트리거를 실제로 붙일 때, 토스트·얼럿이 같은 타이밍에
            // 겹쳐 뜨지 않게 같이 처리한다 — 지금은 그냥 세로로 쌓아 둘 다 보일 수 있다
            overlayContent = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    YGToastHost(policy = toastPolicy, modifier = Modifier.fillMaxWidth())
                    YGAlertHost(policy = alertPolicy, modifier = Modifier.fillMaxWidth())
                }
            },
            isEmpty = canvasState.isCanvasEmpty,
            emptyMessage = stringResource(R.string.canvas_main_empty_message),
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
                    text = stringResource(R.string.canvas_main_camera_capture),
                    onClick = {
                        isMenuExpanded = false
                        onClickCamera()
                    },
                ),
                YGCanvasMenuItem(
                    text = stringResource(R.string.canvas_main_gallery_select),
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
                spotlightedToppingId = canvasState.spotlightedToppingId,
                onClickTopping = onClickTopping,
                onClickSpotlightDim = onClickSpotlightDim,
                // 지난 날을 기다리는 동안에는 직전 캔버스를 그대로 두므로, 고른 날짜가 아니라
                // 실제로 그리고 있는 캔버스가 바뀔 때 다시 모은다. 폴링으로 같은 캔버스가
                // 다시 와도 id 는 그대로라 헛되이 가리지 않는다
                revealResetKey = canvasState.displayedCanvas?.parfaitId,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * 배경이 미설정이거나 앱이 모르는 type 이면 null 이 온다. 색을 못 읽었을 때도 미설정으로
 * 떨어뜨린다 — 캔버스를 못 그리는 것보다 [YGCanvas] 의 기본 배경을 깔아 주는 편이 낫다.
 */
private fun CanvasBackground?.toYGCanvasBackground(): YGCanvasBackground? = when (this) {
    null -> null
    is CanvasBackground.Color -> value.toColorOrNull()?.let(YGCanvasBackground::Solid)
    is CanvasBackground.Image -> YGCanvasBackground.Image(url)
}

private class CanvasMainScreenPreviewParameterProvider :
    PreviewParameterProvider<CanvasMainUiState> {
    private val today = LocalDate(2026, 5, 20)

    private val memberChips = listOf(
        GroupMemberChip(GroupMemberId(1L), "문설빈", YGColorChipType.NametagChip1),
        GroupMemberChip(GroupMemberId(2L), "전계원", YGColorChipType.NametagChip8),
        GroupMemberChip(GroupMemberId(3L), "전희훈", YGColorChipType.NametagChip5),
        GroupMemberChip(GroupMemberId(4L), "장서휘", YGColorChipType.NametagChip3),
        GroupMemberChip(GroupMemberId(5L), "김수연", YGColorChipType.NametagChip11),
        GroupMemberChip(GroupMemberId(6L), "김남수", YGColorChipType.NametagChip6),
        GroupMemberChip(GroupMemberId(7L), "박서연", YGColorChipType.NametagChip2),
    )

    override val values: Sequence<CanvasMainUiState>
        get() = sequenceOf(
            CanvasMainUiState(
                groupName = "그룹이름은최대열글자",
                memberChips = memberChips,
                today = today,
                selectedDate = today,
            ),
            // 지난 캔버스 — 아래 버튼이 저장·오늘로 가기로 바뀐다
            CanvasMainUiState(
                groupName = "그룹이름은최대열글자",
                memberChips = memberChips,
                today = today,
                selectedDate = LocalDate(2026, 5, 3),
            ),
        )
}

@YGPreview
@Composable
private fun PreviewCanvasMainScreen(
    @PreviewParameter(CanvasMainScreenPreviewParameterProvider::class) uiState: CanvasMainUiState,
) = PreviewBox {
    CanvasMainScreen(
        canvasState = uiState,
        onClickBack = {},
        onClickDateSelect = {},
        onClickMenu = {},
        onClickCamera = {},
        onClickGallery = {},
        onClickEditCanvasBG = {},
        onClickSaveToGallery = {},
        onClickGoToToday = {},
        onDismissCalendar = {},
        onSelectYear = {},
        onSelectMonth = {},
        onClickDate = {},
        onClickTopping = {},
        onClickSpotlightDim = {},
        modifier = Modifier.fillMaxSize(),
    )
}

/** 갤러리 저장 토스트가 캔버스 상단에 어떻게 걸리는지 확인용. */
@YGPreview
@Composable
private fun PreviewCanvasMainScreenWithGallerySaveToast() = PreviewBox {
    val today = LocalDate(2026, 5, 20)
    val toastMessage = stringResource(R.string.canvas_main_gallery_save_success, 12, 31)
    val toastPolicy = remember(toastMessage) {
        YGToastPolicy().apply { show(YGToastType.InviteCode(toastMessage)) }
    }

    CanvasMainScreen(
        canvasState = CanvasMainUiState(
            groupName = "그룹이름은최대열글자",
            today = today,
            selectedDate = LocalDate(2026, 5, 3),
        ),
        onClickBack = {},
        onClickDateSelect = {},
        onClickMenu = {},
        onClickCamera = {},
        onClickGallery = {},
        onClickEditCanvasBG = {},
        onClickSaveToGallery = {},
        onClickGoToToday = {},
        onDismissCalendar = {},
        onSelectYear = {},
        onSelectMonth = {},
        onClickDate = {},
        onClickTopping = {},
        onClickSpotlightDim = {},
        toastPolicy = toastPolicy,
        modifier = Modifier.fillMaxSize(),
    )
}

@YGPreview
@Composable
private fun PreviewCanvasMainScreenWithClosedCanvasAlert() = PreviewBox {
    val today = LocalDate(2026, 5, 20)
    val alertTitle = stringResource(R.string.canvas_main_closed_canvas_alert_title, 12, 31)
    val alertSub = stringResource(R.string.canvas_main_closed_canvas_alert_sub, 12)
    val alertButtonText = stringResource(R.string.canvas_main_closed_canvas_alert_button)
    val alertPolicy = remember(alertTitle, alertSub, alertButtonText) {
        YGAlertPolicy().apply {
            show(title = alertTitle, sub = alertSub, buttonText = alertButtonText)
        }
    }

    CanvasMainScreen(
        canvasState = CanvasMainUiState(
            groupName = "그룹이름은최대열글자",
            today = today,
            selectedDate = today,
        ),
        onClickBack = {},
        onClickDateSelect = {},
        onClickMenu = {},
        onClickCamera = {},
        onClickGallery = {},
        onClickEditCanvasBG = {},
        onClickSaveToGallery = {},
        onClickGoToToday = {},
        onDismissCalendar = {},
        onSelectYear = {},
        onSelectMonth = {},
        onClickDate = {},
        onClickTopping = {},
        onClickSpotlightDim = {},
        alertPolicy = alertPolicy,
        modifier = Modifier.fillMaxSize(),
    )
}
