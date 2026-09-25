package com.teamyg.parfait.data.source.image.local

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.datastore.FakePreferencesDataStore
import com.teamyg.parfait.data.model.entity.RecentImageEntity
import com.teamyg.parfait.data.model.entity.RecentImageKindEntity
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
    fun updateThenRead_roundTripsKind() = runTest {
        // Given 종류가 섞인 목록

        // When 저장했다가 읽는다
        dataSource.update { entities }
        val decoded = dataSource.values.first()

        // Then 종류가 뒤바뀌지 않는다
        assertEquals(entities, decoded)
    }

    @Test
    fun values_legacyStringList_survivesAsSourceKind() = runTest {
        // Given 종류 축이 없던 시절의 값이 남아 있다
        val legacy = """["content://recent/a","content://recent/b"]"""

        // When 새 스키마로 읽는다
        seed(legacy)
        val decoded = dataSource.values.first()

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
    fun values_unknownKind_keepsEntryAsSource() = runTest {
        // Given 이 판본이 모르는 종류값이 섞여 있다(뒷 판본에서 만든 값이거나 손상된 값)
        val unknown = """[{"uri":"content://recent/a","kind":"STICKER"}]"""

        // When 읽는다
        seed(unknown)
        val decoded = dataSource.values.first()

        // Then 항목 하나 때문에 목록 전체가 비워지지 않는다 — 기본값이 있어야
        // coerceInputValues 가 흡수한다
        assertEquals(
            listOf(RecentImageEntity(uri = "content://recent/a", kind = RecentImageKindEntity.SOURCE)),
            decoded,
        )
    }

    @Test
    fun values_brokenPayload_readsEmpty() = runTest {
        // Given 어느 스키마로도 읽히지 않는 값
        val broken = "{not json at all"

        // When 읽는다
        seed(broken)
        val decoded = dataSource.values.first()

        // Then 빈 목록이다 — 두 번째 시도까지 실패했을 때만 여기로 온다
        assertEquals(emptyList(), decoded)
    }

    @Test
    fun update_overLegacyPayload_keepsMigratedEntries() = runTest {
        // Given 구 스키마 값이 저장돼 있다
        seed("""["content://recent/a"]""")

        // When 항목을 하나 더한다
        dataSource.update { current -> current + entities[1] }

        // Then 폴백이 트랜잭션 쪽에도 걸린다 — values 만 고치고 update 를 놓치면
        // 추가하는 순간 기존 목록이 날아간다
        assertEquals(
            listOf(
                RecentImageEntity(uri = "content://recent/a", kind = RecentImageKindEntity.SOURCE),
                entities[1],
            ),
            dataSource.values.first(),
        )
    }

    @Test
    fun remove_dropsMatchingUrisOnly() = runTest {
        // Given 두 항목이 저장돼 있다
        dataSource.update { entities }

        // When 하나만 지운다
        dataSource.remove(listOf("content://recent/a"))

        // Then 나머지 하나가 종류를 유지한 채 남는다
        assertEquals(listOf(entities[1]), dataSource.values.first())
    }

    /** 스키마가 다른 원문을 그대로 심는다 — 공개 API 로는 구 스키마 값을 쓸 수 없다 */
    private suspend fun seed(raw: String) {
        dataStore.edit { prefs -> prefs[stringPreferencesKey("recent_image_uris")] = raw }
    }
}
