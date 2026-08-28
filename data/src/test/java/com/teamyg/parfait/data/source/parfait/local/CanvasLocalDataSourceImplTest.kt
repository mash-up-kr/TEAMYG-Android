package com.teamyg.parfait.data.source.parfait.local

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.parfaitToday
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val GROUP_A = GroupId(1L)
private val GROUP_B = GroupId(2L)

class CanvasLocalDataSourceImplTest {
    private fun canvas(parfaitId: Long) = CanvasVO(
        parfaitId = ParfaitId(parfaitId),
        date = parfaitToday(),
        status = CanvasStatus.ACTIVE,
        lastClosedDate = null,
        members = emptyList(),
        background = null,
        toppings = emptyList(),
    )

    @Test
    fun todayCanvas_beforeAnySave_isNull() = runTest {
        val dataSource = CanvasLocalDataSourceImpl()

        dataSource.todayCanvas(GROUP_A).test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun saveTodayCanvas_emitsToThatGroupsSubscriber() = runTest {
        val dataSource = CanvasLocalDataSourceImpl()

        dataSource.todayCanvas(GROUP_A).test {
            assertNull(awaitItem())

            dataSource.saveTodayCanvas(GROUP_A, canvas(100L))

            assertEquals(ParfaitId(100L), awaitItem()?.parfaitId)
        }
    }

    @Test
    fun saveTodayCanvas_forAnotherGroup_doesNotDisturbThisSubscriber() = runTest {
        val dataSource = CanvasLocalDataSourceImpl()

        dataSource.todayCanvas(GROUP_A).test {
            assertNull(awaitItem())

            dataSource.saveTodayCanvas(GROUP_B, canvas(200L))

            expectNoEvents()
        }
    }

    @Test
    fun cachedTodayCanvas_readsWithoutSubscribing() = runTest {
        val dataSource = CanvasLocalDataSourceImpl()
        assertNull(dataSource.cachedTodayCanvas(GROUP_A))

        dataSource.saveTodayCanvas(GROUP_A, canvas(100L))

        assertEquals(ParfaitId(100L), dataSource.cachedTodayCanvas(GROUP_A)?.parfaitId)
    }

    @Test
    fun clear_emptiesEveryGroup() = runTest {
        val dataSource = CanvasLocalDataSourceImpl()
        dataSource.saveTodayCanvas(GROUP_A, canvas(100L))

        dataSource.todayCanvas(GROUP_A).test {
            assertEquals(ParfaitId(100L), awaitItem()?.parfaitId)

            dataSource.clear()

            assertNull(awaitItem())
        }
    }
}
