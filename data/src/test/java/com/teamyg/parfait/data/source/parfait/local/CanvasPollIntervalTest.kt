package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.domain.model.id.GroupId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

private val GROUP = GroupId(1L)
private val OTHER_GROUP = GroupId(2L)

class CanvasPollIntervalTest {
    @Test
    fun current_beforeAnyFeedback_isTheShortestStage() {
        val interval = CanvasPollInterval()

        assertEquals(10.seconds, interval.current(GROUP))
    }

    @Test
    fun onUnchanged_climbsOneStageAtATime() {
        val interval = CanvasPollInterval()

        interval.onUnchanged(GROUP)
        assertEquals(15.seconds, interval.current(GROUP))

        interval.onUnchanged(GROUP)
        assertEquals(20.seconds, interval.current(GROUP))
    }

    @Test
    fun onUnchanged_atTheTop_staysThere() {
        val interval = CanvasPollInterval()

        repeat(10) { interval.onUnchanged(GROUP) }

        // 상한을 넘겨 세면 화면을 오래 열어 둘수록 남의 토핑이 늦게 보인다
        assertEquals(20.seconds, interval.current(GROUP))
    }

    @Test
    fun onChanged_fromTheTop_returnsToTheShortestStage() {
        val interval = CanvasPollInterval()
        repeat(10) { interval.onUnchanged(GROUP) }

        interval.onChanged(GROUP)

        assertEquals(10.seconds, interval.current(GROUP))
    }

    @Test
    fun onReset_fromTheTop_returnsToTheShortestStage() {
        val interval = CanvasPollInterval()
        repeat(10) { interval.onUnchanged(GROUP) }

        interval.onReset(GROUP)

        assertEquals(10.seconds, interval.current(GROUP))
    }

    @Test
    fun stages_ofOneGroup_doNotMoveAnother() {
        val interval = CanvasPollInterval()

        interval.onUnchanged(GROUP)
        interval.onUnchanged(GROUP)

        assertEquals(20.seconds, interval.current(GROUP))
        assertEquals(10.seconds, interval.current(OTHER_GROUP))
    }

    @Test
    fun forgetAll_clearsGroupsThatNobodyReleased() {
        val interval = CanvasPollInterval()
        repeat(10) { interval.onUnchanged(GROUP) }
        repeat(10) { interval.onUnchanged(OTHER_GROUP) }

        interval.forgetAll()

        assertEquals(10.seconds, interval.current(GROUP))
        assertEquals(10.seconds, interval.current(OTHER_GROUP))
    }

    @Test
    fun forget_makesTheNextVisitStartOver() {
        val interval = CanvasPollInterval()
        repeat(10) { interval.onUnchanged(GROUP) }

        interval.forget(GROUP)

        assertEquals(10.seconds, interval.current(GROUP))
    }
}
