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
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.parfait.ParfaitHistory
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.math.abs
import kotlin.time.Clock

data class GroupMemberChip(
    val nickname: String,
    val colorChipType: YGColorChipType,
)

data class CanvasImageAddUiState(
    val groupName: String = "",
    val memberChips: List<GroupMemberChip> = emptyList(),
    val canvasDate: String = "",
    val canvasDay: String = "",
    /** 오늘 캔버스가 아직 없으면 null 이다 — 화면을 여는 것만으로 만들지 않는다 */
    val parfaitId: ParfaitId? = null,
    /** 미설정이면 null. 그때는 [YGCanvas] 의 기본 배경이 그려진다 */
    val canvasBackground: CanvasBackground? = null,
    /** 그리는 순서대로 들고 있다 — positionZ 오름차순이라 뒤쪽이 위에 덮인다 */
    val toppings: List<CanvasToppingVO> = emptyList(),
    val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val selectedDate: LocalDate = today,
    val isCalendarVisible: Boolean = false,
    /** 달력이 보고 있는 달. [selectedDate] 와 달리 날짜를 고르지 않아도 연·월만 옮겨진다 */
    val displayedMonth: LocalDate = today.toFirstDayOfMonth(),
    /** 드롭다운이 위에서 아래로 읽히도록 오름차순으로 들고 있다 */
    val selectableYears: List<Int> = emptyList(),
    /** [displayedMonth] 가 속한 해의 파르페 기록. 최신순이며, 연·월·일 묶음은 여기서 뽑아 쓴다 */
    val parfaitHistories: List<ParfaitHistory> = emptyList(),
    /** [parfaitHistories] 중 이미지가 실제로 올라간 날. 달력이 점을 찍는 기준 */
    val uploadedDates: Set<LocalDate> = emptySet(),
) : UiState {
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

    /** 토핑이 하나도 없을 때만 안내 문구가 뜬다 */
    val isCanvasEmpty: Boolean = toppings.isEmpty()
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
     * 오늘 캔버스가 아직 없으면 빈 화면 그대로 둔다 — 여기서 만들지 않는다. 서버의 오늘 조회는
     * 없으면 캔버스를 생성해 저장하므로, 토핑을 처음 올리는 순간까지 미룬다.
     */
    private fun loadTodayCanvas() {
        launch(key = LOAD_TODAY_CANVAS_KEY) {
            getTodayParfaitUseCase(groupId)
                .onSuccess { canvas ->
                    canvas ?: return@onSuccess

                    updateState {
                        copy(
                            parfaitId = canvas.parfaitId,
                            canvasBackground = canvas.background,
                            toppings = canvas.toppings.sortedBy { it.transform.positionZ },
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
            getParfaitYearsUseCase()
                .onSuccess { years -> updateState { copy(selectableYears = years.sorted()) } }
                .onFailure { throwable ->
                    // 연도 드롭다운이 비는 것뿐이라 실패를 노출하지 않는다
                    viewModelLogger.e(throwable) { "파르페 연도를 불러오지 못했다" }
                }
        }
    }

    private fun loadCanvasImageAddInfo() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        updateState {
            copy(
                // TODO: 그룹 정보 연동 필요 — 캔버스 응답에는 그룹명이 없다
                groupName = "그룹이름은최대열글자",
                canvasDate = today.format(DateTextFormat.monthDayFormat),
                canvasDay = today.format(DateTextFormat.weekdayFormat),
            )
        }
    }

    /**
     * 달력은 연 단위로 한 번만 받아 두고 월을 오갈 때는 다시 부르지 않는다. 빈 파르페(캔버스에
     * 들어가기만 하고 이미지는 안 올린 날)를 걸러 내지 않으면 화면을 열어 본 날마다 점이 찍힌다.
     */
    private fun loadParfaitHistories(
        year: Int,
        moveToNearestMonth: Boolean = false,
    ) {
        launch(key = LOAD_PARFAIT_HISTORIES_KEY) {
            getParfaitHistoriesUseCase(year)
                .onSuccess { histories ->
                    updateState {
                        val loaded = copy(
                            parfaitHistories = histories,
                            uploadedDates = histories
                                .filterNot(ParfaitHistory::isEmpty)
                                .mapTo(mutableSetOf(), ParfaitHistory::date),
                        )

                        if (moveToNearestMonth) loaded.movedToNearestMonth(year) else loaded
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

            is CanvasImageAddIntent.ClickDate -> updateState { copy(selectedDate = intent.date) }
        }
    }

    /** 달을 미리 못 정하는 이유: 그 해에 어떤 달이 있는지는 목록을 받아 봐야 안다 */
    private fun handleSelectYear(year: Int) {
        if (year == state.value.displayedMonth.year) return

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

        /** 상단 Nametag-Chip 색. 서버가 색을 주지 않아 멤버 순서로 돌려 쓴다 */
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
