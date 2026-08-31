package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.jvm.extension.ElapsedTimeBucket
import com.teamyg.parfait.core.util.jvm.extension.toElapsedTimeBucket
import com.teamyg.parfait.core.util.jvm.extension.toFirstDayOfMonth
import com.teamyg.parfait.core.util.jvm.model.DateTextFormat
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasMemberVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.PARFAIT_TIME_ZONE
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import com.teamyg.parfait.domain.usecase.gallery.SaveCanvasToGalleryUseCase
import com.teamyg.parfait.domain.usecase.group.GetMyGroupsFlowUseCase
import com.teamyg.parfait.domain.usecase.group.RefreshMyGroupsUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitHistoriesUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitYearsUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.parfait.ObserveParfaitDayBoundaryUseCase
import com.teamyg.parfait.domain.usecase.parfait.ObserveTodayParfaitRefreshFailureUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.util.toColorChipType
import com.teamyg.parfait.feature.groups.canvas.impl.util.toSpotlightToastNameColor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.math.abs
import kotlin.time.Clock

data class GroupMemberChip(
    val groupMemberId: GroupMemberId,
    val nickname: String,
    val colorChipType: YGColorChipType,
)

data class CanvasMainUiState(
    val groupName: String = "",
    val memberChips: List<GroupMemberChip> = emptyList(),
    /**
     * 오늘 캔버스. 저장소 구독 값이다 — 아직 못 받았거나 조회가 실패했으면 null 이다
     * (`adr/0029-canvas-today-ssot-polling.md`).
     *
     * [pastCanvas] 와 나눠 두는 이유는 토핑 추가·배경 편집이 언제나 오늘 것을 대상으로 해야
     * 해서다. 지난 날을 보다가 그 캔버스를 고치면 서버가 409 로 되돌려준다(`api/parfait.md`).
     */
    val todayCanvas: CanvasVO? = null,
    /**
     * 달력에서 고른 **지난** 날의 캔버스.
     *
     * "지금 그려지는 캔버스"는 [displayedCanvas] 가 맡는다 — 오늘 것은 구독이 채우므로, 둘을
     * 한 칸에 겹쳐 두면 구독 방출이 보고 있던 지난 날을 덮는다.
     */
    val pastCanvas: CanvasVO? = null,
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
    /** Spotlight 로 강조된 토핑. Default 상태면 null */
    val spotlightedToppingId: ParfaitImageId? = null,
    /** 아직 오늘 캔버스를 한 번도 받지 못한 채 도는 조회. 화면을 덮는다 */
    val isInitialLoading: Boolean = false,
) : UiState {
    /** 지난 날 상세를 기다리는 동안에는 직전에 보던 것을 그대로 둔다 */
    val displayedCanvas: CanvasVO?
        get() = if (isViewingToday) todayCanvas else (pastCanvas ?: todayCanvas)

    /** 미설정이면 null. 그때는 [YGCanvas] 의 기본 배경이 그려진다 */
    val canvasBackground: CanvasBackground?
        get() = displayedCanvas?.background

    /** 그리는 순서대로 들고 있다 — positionZ 오름차순이라 뒤쪽이 위에 덮인다 */
    val toppings: List<CanvasToppingVO>
        get() = displayedCanvas?.toppings.orEmpty().sortedBy { it.transform.positionZ }

    /** 지난 날에는 편집 대신 저장·오늘로 가기를 준다 */
    val isViewingToday: Boolean
        get() = selectedDate == today

    /**
     * 열어 두면 촬영·누끼·편집을 다 마친 뒤에야 올릴 데가 없다는 것을 알게 된다
     * (`adr/0026-topping-draft-datastore-ssot.md`).
     */
    val isToppingAddEnabled: Boolean
        get() = isViewingToday && todayCanvas != null

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

    /** Default 상태(또는 스포트라이트된 토핑이 그 사이 지워졌을 때)면 null */
    val spotlightedTopping: CanvasToppingVO?
        get() = spotlightedToppingId?.let { id -> toppings.firstOrNull { it.parfaitImageId == id } }
}

sealed interface CanvasMainEffect : UiSideEffect {
    class NavigateToCamera : CanvasMainEffect

    class NavigateToCanvas : CanvasMainEffect

    data class NavigateToCanvasBGEdit(
        val groupId: GroupId,
        val parfaitId: ParfaitId,
        /** 특정 토핑을 탭해 들어온 경우에만 채운다 — 화면이 토핑 탭에서 그 토핑을 바로 선택해 연다 */
        val toppingId: ParfaitImageId? = null,
    ) : CanvasMainEffect

    data class NavigateToGroupSetting(val groupId: GroupId) : CanvasMainEffect

    /**
     * 지금 보고 있는 캔버스 프레임을 이미지로 캡처해 달라는 요청. 캡처(Compose
     * GraphicsLayer) 자체는 화면만 할 수 있어, ViewModel 은 요청만 보내고 화면이 캡처한
     * 결과를 [CanvasMainIntent.SaveCapturedCanvas] 로 다시 돌려받는다.
     */
    data object RequestCanvasCapture : CanvasMainEffect

    data class ShowGallerySaveResult(
        val isSuccess: Boolean,
        val date: LocalDate,
    ) : CanvasMainEffect

    /** Spotlight 진입과 동시에 1회 노출하는 작성자 정보 토스트 */
    data class ShowSpotlightToast(
        val nickname: String,
        val nicknameColor: Color,
        val elapsed: ElapsedTimeBucket,
    ) : CanvasMainEffect

    /** 첫 조회를 기다리는 동안 갱신이 실패했을 때만 온다 — 폴링은 5초마다 돌아 매번 알리면 방해가 된다 */
    data object ShowTodayCanvasError : CanvasMainEffect

    data object ShowToppingFlowStartError : CanvasMainEffect
}

sealed interface CanvasMainIntent : UiIntent {
    /**
     * 화면이 앞에 섰다. 처음 열릴 때뿐 아니라 다른 화면에서 **돌아올 때마다** 온다 — ViewModel 은
     * NavEntry 가 백스택에 남아 있는 한 살아 있어, 초기화 한 번으로는 캔버스가 낡는다.
     *
     * 캔버스는 다른 멤버가 올린 토핑으로도 바뀌므로, 내 앱 안의 변경만 좇아서는 최신이 될 수
     * 없다. 다만 다시 물어보는 것은 오늘을 보고 있을 때뿐이다 — 지난 날의 캔버스는 마감돼
     * 더 바뀌지 않는다.
     */
    data object Enter : CanvasMainIntent

    class OnClickCamera : CanvasMainIntent

    class OnClickCanvas : CanvasMainIntent

    class OnClickCanvasEdit : CanvasMainIntent

    data object OnClickGroupSetting : CanvasMainIntent

    data object OnClickDateSelect : CanvasMainIntent

    data object DismissCalendar : CanvasMainIntent

    data class SelectYear(val year: Int) : CanvasMainIntent

    data class SelectMonth(val month: LocalDate) : CanvasMainIntent

    data class ClickDate(val date: LocalDate) : CanvasMainIntent

    data object OnClickSaveToGallery : CanvasMainIntent

    data object OnClickGoToToday : CanvasMainIntent

    /** [CanvasMainEffect.RequestCanvasCapture] 에 대한 응답으로, 화면이 캡처한 비트맵을 돌려준다 */
    data class SaveCapturedCanvas(val bitmap: Bitmap) : CanvasMainIntent

    /** 캔버스 위의 토핑 하나를 탭했다. Default 상태에서만 Spotlight 로 전환된다 */
    data class OnClickTopping(val topping: CanvasToppingVO) : CanvasMainIntent

    /** Spotlight 상태에서 강조된 토핑 밖(Dim 영역)을 탭했다 */
    data object OnClickSpotlightDim : CanvasMainIntent

    /** 앱이 백그라운드로 이동했다가 복귀했다. Spotlight 를 해제한다 */
    data object OnAppReturnedFromBackground : CanvasMainIntent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CanvasMainViewModel.Factory::class)
class CanvasMainViewModel
@AssistedInject
constructor(
    @Assisted groupIdValue: Long,
    private val getParfaitHistoriesUseCase: GetParfaitHistoriesUseCase,
    private val getParfaitYearsUseCase: GetParfaitYearsUseCase,
    private val getTodayParfaitFlowUseCase: GetTodayParfaitFlowUseCase,
    private val observeParfaitDayBoundaryUseCase: ObserveParfaitDayBoundaryUseCase,
    private val observeTodayParfaitRefreshFailureUseCase: ObserveTodayParfaitRefreshFailureUseCase,
    private val getParfaitDetailUseCase: GetParfaitDetailUseCase,
    private val getMyGroupsFlowUseCase: GetMyGroupsFlowUseCase,
    private val refreshMyGroupsUseCase: RefreshMyGroupsUseCase,
    private val saveCanvasToGalleryUseCase: SaveCanvasToGalleryUseCase,
    private val toppingDraftRepository: ToppingDraftRepository,
) : BaseViewModel<CanvasMainUiState, CanvasMainIntent, CanvasMainEffect>(
    initialState = CanvasMainUiState(),
) {
    private val groupId = GroupId(groupIdValue)

    /**
     * 지난 날을 보는 동안 구독을 끊기 위한 게이트.
     *
     * [state] 를 읽지 않는 이유는 [launchWhileSubscribed] KDoc 에 있다 — 그 안에서 [state] 를
     * 수집하면 그 수집 자체가 구독자로 세어진다.
     */
    private val isViewingToday = MutableStateFlow(true)

    init {
        viewModelLogger.i { "CanvasMainViewModel::init" }
        observeTodayCanvas()
        observeTodayCanvasRefreshFailure()
        observeDayBoundary()
        loadCanvasMainInfo()
        // 연도 목록은 해가 바뀔 때만 늘어나 재진입마다 물어볼 값이 아니다
        loadParfaitYears()
    }

    /**
     * 오늘 캔버스를 화면으로 옮기는 유일한 길 — 갱신은 폴러가 구독 시작에서 맡는다
     * (`adr/0029-canvas-today-ssot-polling.md`).
     *
     * 지난 날을 보는 동안은 [isViewingToday] 가 [emptyFlow] 로 갈아 끼운다 — 마감된 날은
     * 바뀌지 않으므로 오늘 캔버스를 계속 부를 이유가 없다. 그 사이 받은 마지막 값은 그대로 둔다.
     * 비우면 오늘로 돌아올 때 빈 캔버스가 깜빡인다.
     */
    private fun observeTodayCanvas() {
        launchWhileSubscribed(
            // MutableStateFlow 는 이미 값이 바뀔 때만 emit 하므로 distinctUntilChanged 가 필요 없다
            source = {
                isViewingToday
                    .flatMapLatest { viewingToday ->
                        if (viewingToday) getTodayParfaitFlowUseCase(groupId) else emptyFlow()
                    }.onStart {
                        // 아직 받아 둔 캔버스가 없을 때만 덮는다 — 그러지 않으면 화면에 다시
                        // 붙을 때마다 이미 그려진 캔버스 위로 덮개가 번쩍인다
                        if (state.value.todayCanvas == null) updateState { copy(isInitialLoading = true) }
                    }
            },
        ) { canvas ->
            updateState {
                copy(
                    todayCanvas = canvas,
                    memberChips = canvas?.members?.toMemberChips() ?: memberChips,
                    // 캔버스가 실렸으면 덮개를 내린다. 갱신이 실패해 아무것도 실리지 않는 쪽은
                    // observeTodayCanvasRefreshFailure 가 맡는다
                    isInitialLoading = if (canvas == null) isInitialLoading else false,
                    // 강조 중이던 토핑이 사라지면 딤도 사라져 Dim 탭이라는 해제 계기가 없어진다.
                    // 상태값을 그대로 두면 handleOnClickTopping 가드에 걸려 탭이 전부 먹지 않는다
                    spotlightedToppingId = spotlightedToppingId?.takeIf { id ->
                        canvas?.toppings.orEmpty().any { it.parfaitImageId == id }
                    },
                )
            }
        }
    }

    /**
     * 갱신이 실패하면 캐시가 아무것도 내지 않아 [observeTodayCanvas] 만으로는 덮개를 내릴 계기가
     * 없다 — 첫 조회를 기다리는 동안에만 그 실패를 화면으로 옮긴다.
     *
     * 폴링은 5초마다 도므로 조건 없이 알리면 실패가 이어지는 동안 토스트가 계속 쌓인다.
     * 덮개가 걸려 있을 때로 좁혀 첫 조회 실패 한 번만 말한다.
     */
    private fun observeTodayCanvasRefreshFailure() {
        launchWhileSubscribed(source = { observeTodayParfaitRefreshFailureUseCase(groupId) }) {
            if (state.value.isInitialLoading.not()) return@launchWhileSubscribed

            updateState { copy(isInitialLoading = false) }
            postSideEffect(CanvasMainEffect.ShowTodayCanvasError)
        }
    }

    /**
     * 화면을 열어 둔 채 파르페 하루 경계를 넘기면 오늘을 다시 센다. 진입 시점만으로는 부족하다 —
     * 그 사이 폴링이 받아 온 오늘 캔버스가 어제 날짜 헤더 아래 그려진다.
     */
    private fun observeDayBoundary() {
        launchWhileSubscribed(source = { observeParfaitDayBoundaryUseCase() }) { today ->
            updateState {
                if (today == this.today) return@updateState this

                if (this.isViewingToday) {
                    copy(today = today, selectedDate = today, displayedMonth = today.toFirstDayOfMonth())
                } else {
                    copy(today = today)
                }
            }
            isViewingToday.value = state.value.selectedDate == state.value.today
        }
    }

    /**
     * 오늘 캔버스 갱신은 폴러가 구독 시작에서 이미 한다
     * (`adr/0029-canvas-today-ssot-polling.md`).
     *
     * 달력 기록은 폴링 대상이 아니라 여기서 받는다 — 다른 멤버가 오늘 토핑을 올리면 오늘 칸의
     * 점이 생기는데, 연 단위 캐시는 그것을 스스로 알 방법이 없다. 바뀔 수 있는 해는 올해뿐이다.
     */
    private fun handleEnter() {
        if (state.value.isViewingToday.not()) return
        // 해가 바뀐 직후를 대비해 상태가 아니라 시계에서 읽는다
        loadParfaitHistories(parfaitToday().year)
    }

    /**
     * 색은 서버가 배정한 값을 그대로 쓴다 — 목록 순서로 돌리면 멤버가 빠질 때 남은 사람 색이 밀린다.
     */
    private fun List<CanvasMemberVO>.toMemberChips(): List<GroupMemberChip> = map { member ->
        GroupMemberChip(
            groupMemberId = member.groupMemberId,
            nickname = member.nickname.value,
            colorChipType = member.nametagChip.toColorChipType(),
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

    /**
     * 캔버스 응답에는 그룹명이 없어 그룹 목록 캐시에서 가져온다. 캐시가 비어 있는 진입
     * (프로세스 재시작 후 캔버스로 복귀)에서만 목록을 한 번 받아 온다 — 이름 한 줄 때문에
     * 캔버스를 막지 않으므로 그 조회의 실패는 로그로만 남긴다.
     */
    private fun loadCanvasMainInfo() {
        viewModelScope.launch {
            getMyGroupsFlowUseCase().collect { groups ->
                if (groups == null) return@collect

                val groupName = groups
                    .firstOrNull { it.groupId == groupId }
                    ?.groupName
                    ?.value
                    .orEmpty()
                updateState { copy(groupName = groupName) }
            }
        }

        launch(key = LOAD_GROUP_NAME_KEY) {
            if (getMyGroupsFlowUseCase().first() != null) return@launch

            refreshMyGroupsUseCase().onFailure { throwable ->
                viewModelLogger.e(throwable) { "그룹명을 불러오지 못했다 - groupId: ${groupId.value}" }
            }
        }
    }

    /**
     * 달력은 연 단위로 한 번만 받아 [CanvasMainUiState.parfaitHistoriesByYear] 에 쌓아 두고,
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

    override fun processIntent(intent: CanvasMainIntent) {
        when (intent) {
            is CanvasMainIntent.Enter -> handleEnter()

            is CanvasMainIntent.OnClickCamera -> handleOnClickCamera()

            is CanvasMainIntent.OnClickCanvas -> handleOnClickCanvas()

            is CanvasMainIntent.OnClickCanvasEdit -> handleOnClickCanvasEdit()

            is CanvasMainIntent.OnClickGroupSetting -> postSideEffect(
                effect = CanvasMainEffect.NavigateToGroupSetting(groupId),
            )

            // 달력이 열린 동안에도 같은 버튼이 달력 위에 다시 그려지므로, 한 번 더 누르면 닫는다
            is CanvasMainIntent.OnClickDateSelect -> {
                updateState { copy(isCalendarVisible = isCalendarVisible.not()) }
            }

            is CanvasMainIntent.DismissCalendar -> updateState { copy(isCalendarVisible = false) }

            is CanvasMainIntent.SelectYear -> handleSelectYear(intent.year)

            is CanvasMainIntent.SelectMonth -> updateState {
                copy(displayedMonth = intent.month.toFirstDayOfMonth())
            }

            is CanvasMainIntent.ClickDate -> handleClickDate(intent.date)

            is CanvasMainIntent.OnClickSaveToGallery -> handleClickSaveToGallery()

            is CanvasMainIntent.OnClickGoToToday -> handleClickGoToToday()

            is CanvasMainIntent.SaveCapturedCanvas -> handleSaveCapturedCanvas(intent.bitmap)

            is CanvasMainIntent.OnClickTopping -> handleOnClickTopping(intent.topping)

            is CanvasMainIntent.OnClickSpotlightDim -> resetSpotlight()

            is CanvasMainIntent.OnAppReturnedFromBackground -> resetSpotlight()
        }
    }

    /**
     * 본인 토핑은 Spotlight 대상이 아니라 C-305 토핑 편집으로 이어진다. 나머지는
     * Default → Spotlighted 로만 전환한다.
     */
    private fun handleOnClickTopping(topping: CanvasToppingVO) {
        if (state.value.spotlightedToppingId != null) return

        if (topping.isMine) {
            handleOnClickMyTopping(topping)
            return
        }

        updateState { copy(spotlightedToppingId = topping.parfaitImageId) }

        // TODO(임시): 서버 nameTagChp 필드가 도메인까지 반영되면(데이터 계층 담당) 그 값을
        // 바로 쓰도록 바꾼다. 그 전까지는 화면이 이미 들고 있는 memberChips 에서 같은
        // groupMemberId 를 찾아 대신 쓴다 — 탈퇴·이탈한 멤버는 이 목록에 없어 Default 로 빠진다.
        val chipType = state.value.memberChips
            .firstOrNull { chip -> chip.groupMemberId == topping.placedBy.groupMemberId }
            ?.colorChipType
            ?: YGColorChipType.Default

        postSideEffect(
            effect = CanvasMainEffect.ShowSpotlightToast(
                nickname = topping.placedBy.nickname.value,
                nicknameColor = chipType.toSpotlightToastNameColor(),
                // createdAt 은 타임존이 없는 KST 벽시계 시각이다(PARFAIT_TIME_ZONE 참고) — 기기
                // 시간대로 재면 해외에 있는 기기에서 서버와 어긋난다
                elapsed = topping.createdAt
                    .toInstant(PARFAIT_TIME_ZONE)
                    .toElapsedTimeBucket(Clock.System.now()),
            ),
        )
    }

    /**
     * 오늘 캔버스를 보고 있을 때만 C-305 로 보낸다. 탭한 토핑 id 를 실어 보내
     * 편집 화면이 토핑 탭에서 그 토핑을 바로 선택한 채로 열리게 한다.
     */
    private fun handleOnClickMyTopping(topping: CanvasToppingVO) {
        if (!state.value.isViewingToday) return
        val todayCanvas = state.value.todayCanvas ?: return

        postSideEffect(
            effect = CanvasMainEffect.NavigateToCanvasBGEdit(
                groupId = groupId,
                parfaitId = todayCanvas.parfaitId,
                toppingId = topping.parfaitImageId,
            ),
        )
    }

    /**
     * 백그라운드 복귀·Pull-to-refresh 양쪽에서 재사용할 수 있도록 함수로 분리해 둔다.
     * Pull-to-refresh 는 아직 캔버스 화면에 없어 지금은 [CanvasMainIntent.OnAppReturnedFromBackground]
     * 에서만 부른다.
     */
    private fun resetSpotlight() {
        updateState { copy(spotlightedToppingId = null) }
    }

    /**
     * 오늘만은 부르지 않는다 — 서버의 오늘 조회는 캔버스가 없으면 만들어 저장하므로, 이미
     * 받아 둔 [CanvasMainUiState.todayCanvas] 로 되돌리는 것이 유일하게 안전한 길이다.
     * 되돌리는 대입이 따로 없는 것은 [CanvasMainUiState.displayedCanvas] 가 파생값이라서다.
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
            updateState { copy(selectedDate = date, pastCanvas = null) }
            isViewingToday.value = state.value.selectedDate == state.value.today
            return
        }

        // 달력이 기록 있는 날만 열어 주므로 여기까지 오면 있어야 한다. 없으면 그냥 두는 편이
        // 빈 캔버스를 보여 주는 것보다 낫다
        val parfaitId = current.parfaitHistories.firstOrNull { it.date == date }?.parfaitId ?: return

        updateState { copy(selectedDate = date) }
        isViewingToday.value = state.value.selectedDate == state.value.today
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
                        if (selectedDate == date) copy(pastCanvas = canvas) else this
                    }
                }.onFailure { throwable ->
                    viewModelLogger.e(throwable) { "캔버스를 불러오지 못했다 - date: $date" }
                }
        }
    }

    /**
     * 달력도 오늘이 있는 달로 따라간다. 오늘 캔버스 갱신은 부르지 않는다 —
     * [isViewingToday] 를 다시 세워 구독을 열면 폴러 재구독이 그 자리를 맡는다.
     */
    private fun handleClickGoToToday() {
        updateState {
            copy(
                selectedDate = today,
                displayedMonth = today.toFirstDayOfMonth(),
                pastCanvas = null,
            )
        }
        isViewingToday.value = true

        val current = state.value
        if (current.parfaitHistoriesByYear.containsKey(current.today.year).not()) {
            loadParfaitHistories(current.today.year)
        }
    }

    /** 캡처(Compose GraphicsLayer)는 화면만 할 수 있어, 화면에 요청만 보낸다 */
    private fun handleClickSaveToGallery() {
        postSideEffect(effect = CanvasMainEffect.RequestCanvasCapture)
    }

    private fun handleSaveCapturedCanvas(bitmap: Bitmap) {
        val date = state.value.selectedDate

        launch(key = SAVE_CANVAS_TO_GALLERY_KEY) {
            val displayName = "parfait_${System.currentTimeMillis()}.png"

            saveCanvasToGalleryUseCase(bitmap.toAndroidBitmap(), displayName)
                .onSuccess {
                    postSideEffect(effect = CanvasMainEffect.ShowGallerySaveResult(isSuccess = true, date = date))
                }.onFailure {
                    postSideEffect(effect = CanvasMainEffect.ShowGallerySaveResult(isSuccess = false, date = date))
                }
        }
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
     * 기록이 아니라 [CanvasMainUiState.selectableMonths] 에서 고르는 이유는 그래야
     * 드롭다운에 없는 달로 넘어가지 않아서다 — 그 목록에는 이번 달이 더 들어갈 수 있다.
     */
    private fun CanvasMainUiState.movedToNearestMonth(year: Int): CanvasMainUiState {
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

    private fun handleOnClickCamera() {
        startToppingFlow(effect = CanvasMainEffect.NavigateToCamera())
    }

    private fun handleOnClickCanvas() {
        startToppingFlow(effect = CanvasMainEffect.NavigateToCanvas())
    }

    /**
     * 흐름에 들어서는 순간 초안을 새로 쓴다. 그 뒤에야 화면을 옮긴다 — 이유는
     * [CanvasMainUiState.isToppingAddEnabled] 참고.
     */
    private fun startToppingFlow(effect: CanvasMainEffect) {
        val canvas = state.value.todayCanvas ?: return

        launch(
            key = START_TOPPING_FLOW_KEY,
            onError = { error ->
                viewModelLogger.e { "토핑 초안을 쓰지 못했다 - $error" }
                postSideEffect(CanvasMainEffect.ShowToppingFlowStartError)
            },
        ) {
            toppingDraftRepository.start(
                groupId = groupId,
                parfaitId = canvas.parfaitId,
                nextPositionZ = canvas.nextPositionZ(),
            )
            postSideEffect(effect)
        }
    }

    /** 새 토핑은 언제나 맨 위다. 목록 크기로 세면 지워진 토핑이 있는 캔버스에서 z 가 겹친다 */
    private fun CanvasVO.nextPositionZ(): Int = (toppings.maxOfOrNull { it.transform.positionZ } ?: 0) + 1

    /**
     * 오늘 캔버스를 아직 못 받았으면 열지 않는다 — 편집 화면은 대상 캔버스의 id 위에서만
     * 움직이고, 없는 id 를 지어내면 남의 날 캔버스를 고치게 된다.
     */
    private fun handleOnClickCanvasEdit() {
        val todayCanvas = state.value.todayCanvas ?: run {
            viewModelLogger.e { "오늘 캔버스를 못 받은 채로 편집을 눌렀다 - groupId: ${groupId.value}" }
            return
        }

        postSideEffect(
            effect = CanvasMainEffect.NavigateToCanvasBGEdit(
                groupId = groupId,
                parfaitId = todayCanvas.parfaitId,
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(groupIdValue: Long): CanvasMainViewModel
    }

    private companion object {
        const val LOAD_PARFAIT_HISTORIES_KEY = "loadParfaitHistories"

        const val LOAD_CANVAS_DETAIL_KEY = "loadCanvasDetail"

        const val LOAD_GROUP_NAME_KEY = "loadGroupName"

        const val SAVE_CANVAS_TO_GALLERY_KEY = "saveCanvasToGallery"

        const val START_TOPPING_FLOW_KEY = "startToppingFlow"
    }
}
