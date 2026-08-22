package com.teamyg.parfait.data.source.image.local

import com.teamyg.parfait.data.datastore.FakePreferencesDataStore
import com.teamyg.parfait.data.model.local.RecentImageEntity
import com.teamyg.parfait.data.model.local.RecentImageKindEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class RecentImageLocalDataSourceImplTest {
    private val dataStore = FakePreferencesDataStore()

    // 프로덕션 @LocalJson 과 같은 설정이다(`data/di/JsonModule.kt`). coerceInputValues 를 빼면
    // 모르는 종류값을 흡수하는 동작이 테스트에서 재현되지 않는다
    private val dataSource = RecentImageLocalDataSourceImpl(
        dataStore = dataStore,
        json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        },
    )

    private val entities = listOf(
        RecentImageEntity(uri = "content://recent/a", kind = RecentImageKindEntity.SOURCE),
        RecentImageEntity(uri = "content://recent/b", kind = RecentImageKindEntity.CUTOUT),
    )

    @Test
    fun encodeThenDecode_roundTripsKind() {
        // Given 종류가 섞인 목록

        // When 저장 형태로 바꿨다가 되돌린다
        val decoded = dataSource.decodeValue(dataSource.encodeValue(entities))

        // Then 종류가 뒤바뀌지 않는다
        assertEquals(entities, decoded)
    }

    @Test
    fun decodeValue_legacyStringList_survivesAsSourceKind() {
        // Given 종류 축이 없던 시절의 값이 남아 있다
        val legacy = """["content://recent/a","content://recent/b"]"""

        // When 새 스키마로 읽는다
        val decoded = dataSource.decodeValue(legacy)

        // Then 목록이 통째로 날아가지 않고 원본 사진으로 올라온다 — 여기서 비우면 파일은
        // 남는데 목록에서 사라져 `clearOutsideDayWindow` 가 영영 못 지운다
        assertEquals(
            listOf(
                RecentImageEntity(uri = "content://recent/a", kind = RecentImageKindEntity.SOURCE),
                RecentImageEntity(uri = "content://recent/b", kind = RecentImageKindEntity.SOURCE),
            ),
            decoded,
        )
    }

    @Test
    fun decodeValue_unknownKind_keepsEntryAsSource() {
        // Given 이 판본이 모르는 종류값이 섞여 있다(뒷 판본에서 만든 값이거나 손상된 값)
        val unknown = """[{"uri":"content://recent/a","kind":"STICKER"}]"""

        // When 읽는다
        val decoded = dataSource.decodeValue(unknown)

        // Then 항목 하나 때문에 목록 전체가 비워지지 않는다 — 기본값이 있어야
        // coerceInputValues 가 흡수한다
        assertEquals(
            listOf(RecentImageEntity(uri = "content://recent/a", kind = RecentImageKindEntity.SOURCE)),
            decoded,
        )
    }

    @Test
    fun decodeValue_brokenPayload_readsEmpty() {
        // Given 어느 스키마로도 읽히지 않는 값
        val broken = "{not json at all"

        // When 읽는다
        val decoded = dataSource.decodeValue(broken)

        // Then 빈 목록이다 — 두 번째 시도까지 실패했을 때만 여기로 온다
        assertEquals(emptyList(), decoded)
    }

    @Test
    fun values_afterEditWithLegacyPayload_emitsMigratedList() = runTest {
        // Given 구 스키마 값이 저장돼 있다
        dataSource.edit { editor -> editor.set("""["content://recent/a"]""") }

        // When 흐름을 읽는다
        val emitted = dataSource.values.first()

        // Then 폴백이 흐름 쪽에도 걸린다 — decodeValue 만 고치고 values 를 놓치면
        // 화면에서만 목록이 비어 보인다
        assertEquals(
            listOf(RecentImageEntity(uri = "content://recent/a", kind = RecentImageKindEntity.SOURCE)),
            emitted,
        )
    }

    @Test
    fun remove_dropsMatchingUrisOnly() = runTest {
        // Given 두 항목이 저장돼 있다
        dataSource.edit { editor -> editor.set(dataSource.encodeValue(entities)) }

        // When 하나만 지운다
        dataSource.remove(listOf("content://recent/a"))

        // Then 나머지 하나가 종류를 유지한 채 남는다
        assertEquals(listOf(entities[1]), dataSource.values.first())
    }
}
