package com.teamyg.parfait.feature.groups.list.impl.model

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect
import com.teamyg.parfait.feature.groups.list.impl.model.GroupListEmptyIntroTimeline.DUMMY_DURATION_MILLIS
import com.teamyg.parfait.feature.groups.list.impl.model.GroupListEmptyIntroTimeline.DummyEnterEasing
import com.teamyg.parfait.feature.groups.list.impl.model.GroupListEmptyIntroTimeline.EXIT_DURATION_MILLIS
import com.teamyg.parfait.feature.groups.list.impl.model.GroupListEmptyIntroTimeline.ExitEasing
import com.teamyg.parfait.feature.groups.list.impl.model.GroupListEmptyIntroTimeline.TOOLTIP_DELAY_MILLIS
import com.teamyg.parfait.feature.groups.list.impl.model.GroupListEmptyIntroTimeline.TOOLTIP_DURATION_MILLIS
import com.teamyg.parfait.feature.groups.list.impl.model.GroupListEmptyIntroTimeline.TooltipEnterEasing
import com.teamyg.parfait.feature.groups.list.impl.model.GroupListEmptyIntroTimeline.dummyDelayMillis
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 값 getter 가 [GroupListEmptyIntroPhase] 를 거치므로, [onStop] 은 `Animatable` 을 건드리지 않고
 * 단계만 바꿔도 화면이 완료 상태가 된다. 멈추지 못한 코루틴이 남아도 단계가 값을 덮는다.
 */
@Stable
internal class GroupListEmptyIntroState private constructor(initialPhase: GroupListEmptyIntroPhase) {
    constructor() : this(GroupListEmptyIntroPhase.Entering)

    var phase: GroupListEmptyIntroPhase by mutableStateOf(initialPhase)
        private set

    private val dummies = GROUP_LIST_EMPTY_DUMMY_GROUPS.map { Animatable(0f) }
    private val tooltip = Animatable(0f)
    private val exit = Animatable(1f)

    fun dummyProgress(index: Int): Float = phase.enterValue(dummies[index].value)

    val tooltipAlpha: Float get() = phase.enterValue(tooltip.value)

    val exitAlpha: Float get() = phase.exitValue(exit.value)

    suspend fun play() {
        coroutineScope {
            dummies.forEachIndexed { index, dummy ->
                launch {
                    delay(dummyDelayMillis(index).toLong())
                    dummy.animateTo(1f, tween(DUMMY_DURATION_MILLIS, easing = DummyEnterEasing))
                }
            }
            launch {
                delay(TOOLTIP_DELAY_MILLIS.toLong())
                tooltip.animateTo(1f, tween(TOOLTIP_DURATION_MILLIS, easing = TooltipEnterEasing))
            }
        }
        if (phase == GroupListEmptyIntroPhase.Entering) phase = GroupListEmptyIntroPhase.Shown
    }

    suspend fun dismiss() {
        exit.animateTo(0f, tween(EXIT_DURATION_MILLIS, easing = ExitEasing))
        if (phase == GroupListEmptyIntroPhase.Dismissing) phase = GroupListEmptyIntroPhase.Dismissed
    }

    fun onTouchDown() {
        phase = phase.onTouchDown()
    }

    fun onClickAddGroup() {
        phase = phase.onClickAddGroup()
    }

    fun onStop() {
        phase = phase.onStop()
    }

    companion object {
        /** 프리뷰용. 애니메이션 없이 그 단계로 시작한다 */
        fun settled(phase: GroupListEmptyIntroPhase): GroupListEmptyIntroState = GroupListEmptyIntroState(phase)
    }
}

@Composable
internal fun rememberGroupListEmptyIntroState(enabled: Boolean): GroupListEmptyIntroState {
    val state = remember(enabled) { GroupListEmptyIntroState() }

    LaunchedEffect(state, enabled) {
        if (enabled && state.phase == GroupListEmptyIntroPhase.Entering) state.play()
    }

    LaunchedEffect(state, state.phase) {
        if (state.phase == GroupListEmptyIntroPhase.Dismissing) state.dismiss()
    }

    LifecycleStartEffect(state) {
        onStopOrDispose { state.onStop() }
    }

    return state
}
