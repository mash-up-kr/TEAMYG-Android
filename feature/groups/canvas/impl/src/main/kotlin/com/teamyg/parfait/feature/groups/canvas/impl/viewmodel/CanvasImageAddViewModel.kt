package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.jvm.model.DateTextFormat
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import javax.inject.Inject

data class GroupMemberChip(
    val nickname: String,
    val colorChipType: YGColorChipType,
)

data class CanvasImageAddUiState(
    val groupName: String = "",
    val memberChips: List<GroupMemberChip> = emptyList(),
    val canvasDate: String = "",
    val canvasDay: String = "",
    val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val selectedDate: LocalDate = today,
    val isCalendarVisible: Boolean = false,
) : UiState

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
}

@HiltViewModel
class CanvasImageAddViewModel
@Inject
constructor(
    private val addRecentImageUseCase: AddRecentImageUseCase,
) : BaseViewModel<CanvasImageAddUiState, CanvasImageAddIntent, CanvasImageAddEffect>(
    initialState = CanvasImageAddUiState(),
) {
    init {
        viewModelLogger.i { "CanvasImageAddViewModel::init" }
        loadCanvasImageAddInfo()
    }

    private fun loadCanvasImageAddInfo() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        updateState {
            copy(
                // TODO: 그룹 정보 연동 필요
                groupName = "그룹이름은최대열글자",
                // TODO: 그룹원 Nametag-Chip 정보 연동 필요
                memberChips = listOf(
                    GroupMemberChip("문", YGColorChipType.NametagChip1),
                    GroupMemberChip("전", YGColorChipType.NametagChip8),
                    GroupMemberChip("김", YGColorChipType.NametagChip5),
                    GroupMemberChip("이", YGColorChipType.NametagChip3),
                    GroupMemberChip("박", YGColorChipType.NametagChip11),
                    GroupMemberChip("최", YGColorChipType.NametagChip6),
                    GroupMemberChip("정", YGColorChipType.NametagChip2),
                ),
                canvasDate = today.format(DateTextFormat.monthDayFormat),
                canvasDay = today.format(DateTextFormat.weekdayFormat),
            )
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
        }
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
}
