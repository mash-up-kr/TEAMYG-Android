package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.jvm.extension.toFirstDayOfMonth
import com.teamyg.parfait.core.util.jvm.model.DateTextFormat
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasMemberVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitHistoriesUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitYearsUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.plus
import kotlin.math.abs

data class GroupMemberChip(
    val nickname: String,
    val colorChipType: YGColorChipType,
)

data class CanvasImageAddUiState(
    val groupName: String = "",
    val memberChips: List<GroupMemberChip> = emptyList(),
    /**
     * 오늘 캔버스. 아직 못 받았으면 null 이다 — 조회가 실패했거나 응답 전이다.
     *
     * [viewedCanvas] 와 나눠 두는 이유는 토핑 추가·배경 편집이 언제나 오늘 것을 대상으로
     * 해야 해서다. 서버는 마감된 캔버스의 편집도 막지 않으므로 여기서 갈라 두지 않으면
     * 지난 날을 보다가 그 캔버스를 고치게 된다.
     */
    val todayCanvas: CanvasVO? = null,
    /** 달력에서 고른 날의 캔버스. 화면에 그려지는 것은 언제나 이쪽이다 */
    val viewedCanvas: CanvasVO? = null,
    val today: LocalDate = parfaitToday(),
    val selectedDate: LocalDate = today,
    val isCalendarVisible: Boolean = false,
    /** 달력이 보고 있는 달. [selectedDate] 와 달리 날짜를 고르지 않아도 연·월만 옮겨진다 */
    val displayedMonth: LocalDate = today.toFirstDayOfMonth(),
    /** 드롭다운이 위에서 아래로 읽히도록 오름차순으로 들고 있다 */
    val selectableYears: List<Int> = emptyList(),
    /**
     * 연도별 파르페 기록 캐시. 한 번 받은 해는 다시 부르지 않는다 — 달력은 연·월을 오가는
     * 동안 같은 해로 몇 번이고 돌아온다.
     *
     * 리스트 하나에 이어 붙이지 않는 이유는 "받아 봤는데 비어 있는 해"와 "아직 안 받은 해"를
     * 구분해야 해서다. 그 둘이 같아지면 빈 해를 볼 때마다 서버를 다시 부른다.
     */
    val parfaitHistoriesByYear: Map<Int, List<PastCanvasVO>> = emptyMap(),
) : UiState {
    /** 미설정이면 null. 그때는 [YGCanvas] 의 기본 배경이 그려진다 */
    val canvasBackground: CanvasBackground?
        get() = viewedCanvas?.background

    /** 그리는 순서대로 들고 있다 — positionZ 오름차순이라 뒤쪽이 위에 덮인다 */
    val toppings: List<CanvasToppingVO>
        get() = viewedCanvas?.toppings.orEmpty().sortedBy { it.transform.positionZ }

    /** 지난 날에는 편집 대신 저장·오늘로 가기를 준다 */
    val isViewingToday: Boolean
        get() = selectedDate == today

    val canvasDate: String
        get() = selectedDate.format(DateTextFormat.monthDayFormat)

    val canvasDay: String
        get() = selectedDate.format(DateTextFormat.weekdayFormat)

    /** 최신순. 아직 안 받은 해도 빈 목록이라 "기록 없는 해"와 구분되지 않는다 */
    val parfaitHistories: List<PastCanvasVO>
        get() = parfaitHistoriesByYear[displayedMonth.year].orEmpty()

    /** [parfaitHistories] 중 토핑이 실제로 올라간 날. 달력이 점을 찍는 기준 */
    val uploadedDates: Set<LocalDate> = parfaitHistories
        .filterNot(PastCanvasVO::isEmpty)
        .mapTo(mutableSetOf(), PastCanvasVO::date)

    /**
     * 그 해에 파르페가 있는 달만 고를 수 있다. 이번 달만 예외로, 기록이 없어도 넣는다 —
     * 파르페를 아직 하나도 안 만든 달이어도 오늘로는 돌아올 수 있어야 한다.
     */
    val selectableMonths: List<LocalDate> = parfaitHistories
        .map { it.date.toFirstDayOfMonth() }
        .let { months ->
            val thisMonth = today.toFirstDayOfMonth()
            if (displayedMonth.year == thisMonth.year) months + thisMonth else months
        }.distinct()
        .sorted()

    val isCanvasEmpty: Boolean
        get() = toppings.isEmpty()
}

sealed interface CanvasImageAddEffect : UiSideEffect {
    class NavigateToCamera : CanvasImageAddEffect

    class NavigateToCanvas : CanvasImageAddEffect

    class NavigateToCanvasBGEdit : CanvasImageAddEffect

    data class NavigateToSegmentation(
        val uri: String,
    ) : CanvasImageAddEffect
}

sealed interface CanvasImageAddIntent : UiIntent {
    data class CacheImage(
        val uri: String,
    ) : CanvasImageAddIntent

    class OnClickCamera : CanvasImageAddIntent

    class OnClickCanvas : CanvasImageAddIntent

    class OnClickCanvasEdit : CanvasImageAddIntent

    data object OnClickDateSelect : CanvasImageAddIntent

    data object DismissCalendar : CanvasImageAddIntent

    data class SelectYear(val year: Int) : CanvasImageAddIntent

    data class SelectMonth(val month: LocalDate) : CanvasImageAddIntent

    data class ClickDate(val date: LocalDate) : CanvasImageAddIntent

    data object OnClickSaveToGallery : CanvasImageAddIntent

    data object OnClickGoToToday : CanvasImageAddIntent
}

@HiltViewModel(assistedFactory = CanvasImageAddViewModel.Factory::class)
class CanvasImageAddViewModel
@AssistedInject
constructor(
    @Assisted groupIdValue: Long,
    private val addRecentImageUseCase: AddRecentImageUseCase,
    private val getParfaitHistoriesUseCase: GetParfaitHistoriesUseCase,
    private val getParfaitYearsUseCase: GetParfaitYearsUseCase,
    private val getTodayParfaitUseCase: GetTodayParfaitUseCase,
    private val getParfaitDetailUseCase: GetParfaitDetailUseCase,
) : BaseViewModel<CanvasImageAddUiState, CanvasImageAddIntent, CanvasImageAddEffect>(
    initialState = CanvasImageAddUiState(),
) {
    private val groupId = GroupId(groupIdValue)

    init {
        viewModelLogger.i { "CanvasImageAddViewModel::init" }
        loadCanvasImageAddInfo()
        loadTodayCanvas()
        loadParfaitYears()
        loadParfaitHistories(state.value.today.year)
    }

    /**
     * ⚠️ 서버의 오늘 조회는 캔버스가 없으면 만들어 저장한다 — 화면을 여는 것만으로 그날 캔버스가
     * 생긴다. 그래서 [LOAD_TODAY_CANVAS_KEY] 로 중복 호출을 막고 진입 시 한 번만 부른다.
     */
    private fun loadTodayCanvas() {
        launch(key = LOAD_TODAY_CANVAS_KEY) {
            getTodayParfaitUseCase(groupId)
                .onSuccess { canvas ->
                    updateState {
                        copy(
                            todayCanvas = canvas,
                            // 응답을 기다리는 사이 지난 날로 옮겼다면 그 화면을 덮지 않는다
                            viewedCanvas = if (isViewingToday) canvas else viewedCanvas,
                            memberChips = canvas.members.toMemberChips(),
                        )
                    }
                }.onFailure { throwable ->
                    viewModelLogger.e(throwable) { "오늘 캔버스를 불러오지 못했다 - groupId: ${groupId.value}" }
                }
        }
    }

    /**
     * 서버가 멤버 색을 주지 않아 목록 순서로 팔레트를 돌려 쓴다. 순서가 고정이라 같은 그룹을
     * 다시 열어도 같은 사람에게 같은 색이 간다.
     */
    private fun List<CanvasMemberVO>.toMemberChips(): List<GroupMemberChip> = mapIndexed { index, member ->
        GroupMemberChip(
            nickname = member.nickname.value,
            colorChipType = NAMETAG_CHIP_PALETTE[index % NAMETAG_CHIP_PALETTE.size],
        )
    }

    private fun loadParfaitYears() {
        launch {
            getParfaitYearsUseCase(groupId)
                .onSuccess { years -> updateState { copy(selectableYears = years.sorted()) } }
                .onFailure { throwable ->
                    // 연도 드롭다운이 비는 것뿐이라 실패를 노출하지 않는다
                    viewModelLogger.e(throwable) { "파르페 연도를 불러오지 못했다" }
                }
        }
    }

    private fun loadCanvasImageAddInfo() {
        updateState {
            // TODO: 그룹 정보 연동 필요 — 캔버스 응답에는 그룹명이 없다
            copy(groupName = "그룹이름은최대열글자")
        }
    }

    /**
     * 달력은 연 단위로 한 번만 받아 [CanvasImageAddUiState.parfaitHistoriesByYear] 에 쌓아 두고,
     * 월을 오갈 때는 물론 이미 본 해로 돌아올 때도 다시 부르지 않는다.
     */
    private fun loadParfaitHistories(
        year: Int,
        moveToNearestMonth: Boolean = false,
    ) {
        // 응답을 기다리는 사이 사용자가 다른 해로 옮겼는지 판단할 기준
        val requestedFrom = state.value.displayedMonth.year

        launch(key = LOAD_PARFAIT_HISTORIES_KEY) {
            getParfaitHistoriesUseCase(groupId = groupId, year = year)
                .onSuccess { histories ->
                    updateState {
                        val cached = copy(
                            parfaitHistoriesByYear = parfaitHistoriesByYear + (year to histories),
                        )

                        // 기다리는 동안 사용자가 캐시된 다른 해로 옮겼다면 화면을 되돌리지 않는다
                        if (moveToNearestMonth && displayedMonth.year == requestedFrom) {
                            cached.movedToNearestMonth(year)
                        } else {
                            cached
                        }
                    }
                }.onFailure { throwable ->
                    // 달력의 점이 안 찍힐 뿐 화면은 그대로 쓸 수 있어 실패를 노출하지 않는다
                    viewModelLogger.e(throwable) { "파르페 기록을 불러오지 못했다 - year: $year" }
                }
        }
    }

    override fun processIntent(intent: CanvasImageAddIntent) {
        when (intent) {
            is CanvasImageAddIntent.CacheImage -> handleCacheImage(intent)

            is CanvasImageAddIntent.OnClickCamera -> handleOnClickCamera()

            is CanvasImageAddIntent.OnClickCanvas -> handleOnClickCanvas()

            is CanvasImageAddIntent.OnClickCanvasEdit -> handleOnClickCanvasEdit()

            // 달력이 열린 동안에도 같은 버튼이 달력 위에 다시 그려지므로, 한 번 더 누르면 닫는다
            is CanvasImageAddIntent.OnClickDateSelect -> {
                updateState { copy(isCalendarVisible = isCalendarVisible.not()) }
            }

            is CanvasImageAddIntent.DismissCalendar -> updateState { copy(isCalendarVisible = false) }

            is CanvasImageAddIntent.SelectYear -> handleSelectYear(intent.year)

            is CanvasImageAddIntent.SelectMonth -> updateState {
                copy(displayedMonth = intent.month.toFirstDayOfMonth())
            }

            is CanvasImageAddIntent.ClickDate -> handleClickDate(intent.date)

            is CanvasImageAddIntent.OnClickSaveToGallery -> handleClickSaveToGallery()

            is CanvasImageAddIntent.OnClickGoToToday -> handleClickGoToToday()
        }
    }

    /**
     * 오늘만은 부르지 않는다 — 서버의 오늘 조회는 캔버스가 없으면 만들어 저장하므로, 이미
     * 받아 둔 [CanvasImageAddUiState.todayCanvas] 로 되돌리는 것이 유일하게 안전한 길이다.
     *
     * 응답이 올 때까지 이전 날의 토핑을 그대로 둔다. 비워 두면 파르페가 있는 날인데도 잠깐
     * "캔버스가 비어 있다"고 말하게 된다 — 달력이 기록 있는 날만 열어 주므로 그건 늘 거짓이다.
     */
    private fun handleClickDate(date: LocalDate) {
        val current = state.value

        // 이미 그려져 있는 날을 다시 눌러도 닫는다
        updateState { copy(isCalendarVisible = false) }
        if (date == current.selectedDate) return

        if (date == current.today) {
            updateState { copy(selectedDate = date, viewedCanvas = todayCanvas) }
            return
        }

        // 달력이 기록 있는 날만 열어 주므로 여기까지 오면 있어야 한다. 없으면 그냥 두는 편이
        // 빈 캔버스를 보여 주는 것보다 낫다
        val parfaitId = current.parfaitHistories.firstOrNull { it.date == date }?.parfaitId ?: return

        updateState { copy(selectedDate = date) }
        loadCanvasDetail(date = date, parfaitId = parfaitId)
    }

    private fun loadCanvasDetail(
        date: LocalDate,
        parfaitId: ParfaitId,
    ) {
        launch(key = LOAD_CANVAS_DETAIL_KEY) {
            getParfaitDetailUseCase(groupId = groupId, parfaitId = parfaitId)
                .onSuccess { canvas ->
                    updateState {
                        // 기다리는 사이 다른 날로 옮겼으면 그 날의 캔버스를 덮지 않는다
                        if (selectedDate == date) copy(viewedCanvas = canvas) else this
                    }
                }.onFailure { throwable ->
                    viewModelLogger.e(throwable) { "캔버스를 불러오지 못했다 - date: $date" }
                }
        }
    }

    /** 달력도 오늘이 있는 달로 따라간다 */
    private fun handleClickGoToToday() {
        updateState {
            copy(
                selectedDate = today,
                displayedMonth = today.toFirstDayOfMonth(),
                viewedCanvas = todayCanvas,
            )
        }

        val current = state.value
        if (current.parfaitHistoriesByYear.containsKey(current.today.year).not()) {
            loadParfaitHistories(current.today.year)
        }
    }

    private fun handleClickSaveToGallery() {
        // TODO: 보고 있는 캔버스를 이미지로 만들어 갤러리에 저장한다
        viewModelLogger.i { "갤러리 저장은 아직 구현 전이다" }
    }

    /** 달을 미리 못 정하는 이유: 그 해에 어떤 달이 있는지는 목록을 받아 봐야 안다 */
    private fun handleSelectYear(year: Int) {
        val current = state.value
        if (year == current.displayedMonth.year) return

        // 이미 받아 둔 해면 서버를 거치지 않고 바로 옮긴다
        if (current.parfaitHistoriesByYear.containsKey(year)) {
            updateState { movedToNearestMonth(year) }
            return
        }

        loadParfaitHistories(year = year, moveToNearestMonth = true)
    }

    /**
     * 해를 옮겨도 보고 있던 달을 지킨다. 보고 있던 달을 [year] 로 그대로 옮긴 자리를 기준으로
     * 삼으므로, 그 해에 같은 달이 있으면 거리가 0 이라 저절로 유지되고 없을 때만 옮겨간다.
     *
     * 기록이 아니라 [CanvasImageAddUiState.selectableMonths] 에서 고르는 이유는 그래야
     * 드롭다운에 없는 달로 넘어가지 않아서다 — 그 목록에는 이번 달이 더 들어갈 수 있다.
     */
    private fun CanvasImageAddUiState.movedToNearestMonth(year: Int): CanvasImageAddUiState {
        val anchored = copy(
            displayedMonth = displayedMonth.plus(DatePeriod(years = year - displayedMonth.year)),
        )
        val anchor = anchored.displayedMonth

        return anchored.copy(
            displayedMonth = anchored.selectableMonths
                // 거리가 같으면 더 최근 달로 간다
                .minWithOrNull(compareBy<LocalDate> { abs(it.monthsUntil(anchor)) }.thenByDescending { it })
                ?: anchor,
        )
    }

    private fun handleCacheImage(intent: CanvasImageAddIntent.CacheImage) {
        viewModelScope.launch {
            addRecentImageUseCase(intent.uri)
            postSideEffect(
                effect = CanvasImageAddEffect.NavigateToSegmentation(intent.uri),
            )
        }
    }

    private fun handleOnClickCamera() {
        postSideEffect(
            effect = CanvasImageAddEffect.NavigateToCamera(),
        )
    }

    private fun handleOnClickCanvas() {
        postSideEffect(
            effect = CanvasImageAddEffect.NavigateToCanvas(),
        )
    }

    private fun handleOnClickCanvasEdit() {
        postSideEffect(
            effect = CanvasImageAddEffect.NavigateToCanvasBGEdit(),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(groupIdValue: Long): CanvasImageAddViewModel
    }

    private companion object {
        const val LOAD_PARFAIT_HISTORIES_KEY = "loadParfaitHistories"

        const val LOAD_TODAY_CANVAS_KEY = "loadTodayCanvas"

        const val LOAD_CANVAS_DETAIL_KEY = "loadCanvasDetail"

        /** 상단 Nametag-Chip 색. 고르는 규칙은 [toMemberChips] 에 있다 */
        val NAMETAG_CHIP_PALETTE = listOf(
            YGColorChipType.NametagChip1,
            YGColorChipType.NametagChip2,
            YGColorChipType.NametagChip3,
            YGColorChipType.NametagChip5,
            YGColorChipType.NametagChip6,
            YGColorChipType.NametagChip8,
            YGColorChipType.NametagChip11,
        )
    }
}
