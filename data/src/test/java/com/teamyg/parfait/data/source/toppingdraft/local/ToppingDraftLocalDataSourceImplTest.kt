package com.teamyg.parfait.data.source.toppingdraft.local

import com.teamyg.parfait.data.datastore.FakePreferencesDataStore
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToppingDraftLocalDataSourceImplTest {
    private val dataStore = FakePreferencesDataStore()

    private val dataSource = ToppingDraftLocalDataSourceImpl(
        dataStore = dataStore,
        json = Json { ignoreUnknownKeys = true },
    )

    private val filledDraft = ToppingDraft(
        groupId = GroupId(1L),
        parfaitId = ParfaitId(2L),
        nextPositionZ = 4,
        subjectImagePath = "/data/user/0/com.teamyg.parfait/cache/segmentation/subject.png",
        cutoutImagePath = "/data/user/0/com.teamyg.parfait/cache/segmentation/cutout.png",
        borderColorArgb = 0xFFFF6B6B.toInt(),
        borderWidthDp = 4f,
    )

    @Test
    fun save_thenRead_roundTripsEveryField() = runTest {
        // Given 이미지와 테두리까지 다 채워진 초안

        // When 저장하고 다시 읽는다
        dataSource.save(filledDraft)

        // Then 필드가 하나도 뒤바뀌지 않는다 — 값 클래스 둘과 널 넷을 거쳐 오므로 매퍼가
        // 뒤집혀도 컴파일러가 막지 못한다
        assertEquals(filledDraft, dataSource.draft.first())
    }

    @Test
    fun save_overExistingDraft_replacesItWhole() = runTest {
        // Given 이미지까지 채워진 지난 흐름의 초안이 남아 있다
        dataSource.save(filledDraft)

        // When 캔버스 식별값만 든 새 초안을 얹는다
        val fresh = ToppingDraft(
            groupId = GroupId(9L),
            parfaitId = ParfaitId(8L),
            nextPositionZ = 1,
        )
        dataSource.save(fresh)

        // Then 지난 흐름의 이미지가 따라붙지 않는다 — 병합이 아니라 통째로 덮어쓴다
        assertEquals(fresh, dataSource.draft.first())
    }

    @Test
    fun clear_afterSave_readsNull() = runTest {
        // Given 저장된 초안
        dataSource.save(filledDraft)

        // When 비운다
        dataSource.clear()

        // Then 흐름 밖과 같은 상태가 된다
        assertNull(dataSource.draft.first())
    }

    @Test
    fun draft_nothingSaved_isNull() = runTest {
        // Given, When 아무것도 저장하지 않았다

        // Then 빈 값이 아니라 null 이다 — 없는 초안을 기본값으로 지어내면 읽는 쪽이
        // 있지도 않은 캔버스에 올리려 든다
        assertNull(dataSource.draft.first())
    }

    @Test
    fun draft_storedFormatIsUnreadable_isNull() = runTest {
        // Given 앱 판올림 전 형태로 저장돼 지금은 못 읽는 값
        dataStore.putRaw(ToppingDraftLocalDataSourceImpl.TOPPING_DRAFT_KEY_NAME, "{\"groupId\":")

        // Then 터지지 않고 초안이 없는 것으로 본다 — 흐름은 진입에서 다시 열린다
        assertNull(dataSource.draft.first())
    }
}
